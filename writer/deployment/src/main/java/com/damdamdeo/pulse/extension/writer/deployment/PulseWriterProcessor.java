package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.core.AggregateIdGenerator;
import com.damdamdeo.pulse.extension.core.command.JvmCommandHandlerRegistry;
import com.damdamdeo.pulse.extension.writer.deployment.items.IdentifiableBuildItem;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultInstantProvider;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultQuarkusTransaction;
import com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresSequenceGenerator;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class PulseWriterProcessor {

    private static final String FEATURE = "pulse-writer-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        return List.of(
                AdditionalBeanBuildItem.builder()
                        .addBeanClasses(DefaultQuarkusTransaction.class, DefaultInstantProvider.class,
                                AggregateIdGenerator.class,
                                JdbcPostgresSequenceGenerator.class)
                        .build(),
                // TODO it is not possible to define the bean with @DefaultBean
                // Should conditionally add it if no other implementation is present.
                AdditionalBeanBuildItem.builder()
                        .addBeanClasses(JvmCommandHandlerRegistry.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .build()
        );
    }

    @BuildStep
    void generateAdditionalVolumeBuildItem(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                           final List<IdentifiableBuildItem> identifiableBuildItems,
                                           final BuildProducer<AdditionalVolumeBuildItem> additionalVolumeBuildItemBuildProducer) {
        final String schemaName = applicationInfoBuildItem.getName().toLowerCase();
        final String sequences = identifiableBuildItems.stream().map(IdentifiableBuildItem::identifiableClazz)
                .map(JdbcPostgresSequenceGenerator::sequenceNameFor)
                .map(sequenceName ->
                        // language=sql
                        """
                                CREATE SCHEMA IF NOT EXISTS %1$s;
                                CREATE SEQUENCE IF NOT EXISTS %1$s.%2$s
                                START WITH 1
                                INCREMENT BY 1
                                MINVALUE 1
                                CACHE 1;
                                """.formatted(schemaName, sequenceName))
                .collect(Collectors.joining());
        additionalVolumeBuildItemBuildProducer.produce(new AdditionalVolumeBuildItem(
                new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME),
                new ComposeServiceBuildItem.Volume("./%s_sequences.sql".formatted(schemaName), "/docker-entrypoint-initdb.d/%s_sequences.sql".formatted(schemaName),
                        sequences.getBytes(StandardCharsets.UTF_8))));
        final String sequenceByAggregateRootTypeAndBelongsTo =
                // language=sql
                """
                        CREATE SCHEMA IF NOT EXISTS %1$s;
                        CREATE TABLE %1$s.sequence_by_identifiable_clazz_and_belongs_to (
                            identifiable_clazz character varying(255) not null,
                            belongs_to character varying(255) not null,
                            next_value bigint not null check (next_value > 0),
                            CONSTRAINT identifiable_clazz_and_belongs_to_pkey PRIMARY KEY (identifiable_clazz, belongs_to)
                        );
                        
                        CREATE OR REPLACE FUNCTION %1$s.next_sequence_by_identifiable_clazz_and_belongs_to_value(
                            p_identifiable_clazz TEXT,
                            p_belongs_to TEXT
                        )
                        RETURNS BIGINT
                        LANGUAGE plpgsql
                        AS $$
                        DECLARE
                            v_next BIGINT;
                        BEGIN
                            INSERT INTO %1$s.sequence_by_identifiable_clazz_and_belongs_to (
                                identifiable_clazz,
                                belongs_to,
                                next_value
                            )
                            VALUES (
                                p_identifiable_clazz,
                                p_belongs_to,
                                1
                            )
                            ON CONFLICT (
                                identifiable_clazz,
                                belongs_to
                            )
                            DO UPDATE
                                SET next_value =
                                    sequence_by_identifiable_clazz_and_belongs_to.next_value + 1
                            RETURNING next_value
                            INTO v_next;
                        
                            RETURN v_next;
                        END;
                        $$;
                        """.formatted(schemaName);
        additionalVolumeBuildItemBuildProducer.produce(new AdditionalVolumeBuildItem(
                new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME),
                new ComposeServiceBuildItem.Volume("./%s_sequences_table.sql".formatted(schemaName), "/docker-entrypoint-initdb.d/%s_sequences_table.sql".formatted(schemaName),
                        sequenceByAggregateRootTypeAndBelongsTo.getBytes(StandardCharsets.UTF_8))));
        final String aggregates =
                // language=sql
                """
                        CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
                        CREATE SCHEMA IF NOT EXISTS %1$s;
                        
                        DO $MAIN$
                        BEGIN
                        
                          CREATE TABLE IF NOT EXISTS %1$s.aggregate_root (
                            aggregate_root_type character varying(255) not null,
                            aggregate_root_id character varying(255) not null,
                            last_version bigint not null,
                            aggregate_root_payload bytea NOT NULL CHECK (octet_length(aggregate_root_payload) <= 1000 * 1024),
                            owned_by character varying(255) not null,
                            belongs_to character varying(255) not null,
                            CONSTRAINT aggregate_root_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type)
                          );
                        
                          CREATE TABLE IF NOT EXISTS %1$s.event (
                            aggregate_root_type character varying(255) not null,
                            aggregate_root_id character varying(255) not null,
                            version bigint not null,
                            stored_at timestamptz not null,
                            event_type character varying(255) not null,
                            event_payload bytea not null CHECK (octet_length(event_payload) <= 1000 * 1024),
                            owned_by character varying(255) not null,
                            belongs_to character varying(255) not null,
                            executed_by character varying(255) not null,
                            CONSTRAINT event_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type, version),
                            CONSTRAINT event_unique UNIQUE (aggregate_root_id, aggregate_root_type, version),
                            CONSTRAINT executed_by_format_chk CHECK (executed_by = 'A' OR executed_by LIKE 'EU:%%' OR executed_by LIKE 'SA:%%' OR executed_by = 'NA')
                          );
                        
                          CREATE INDEX IF NOT EXISTS idx_event_aggregate_root_identifier
                            ON %1$s.event USING BTREE (aggregate_root_id, aggregate_root_type);
                        
                          --------------------------------------------------------------------------
                          -- event_check_version_on_create
                          --------------------------------------------------------------------------
                          IF EXISTS (
                            SELECT 1 FROM information_schema.routines
                            WHERE routine_name = 'event_check_version_on_create'
                              AND routine_schema = '%1$s'
                          ) THEN
                            RAISE NOTICE 'Routine event_check_version_on_create EXISTS';
                          ELSE
                            CREATE FUNCTION %1$s.event_check_version_on_create() RETURNS trigger
                              LANGUAGE plpgsql AS $_$
                            DECLARE
                              event_count INTEGER;
                              last_version INTEGER;
                              expected_version INTEGER;
                            BEGIN
                              SELECT COUNT(*)
                                INTO event_count
                                FROM %1$s.event
                                WHERE aggregate_root_id = NEW.aggregate_root_id
                                  AND aggregate_root_type = NEW.aggregate_root_type;
                        
                              IF NEW.version = 0 AND event_count > 0 THEN
                                RAISE EXCEPTION
                                 'Event already present while should not be ! aggregate_root_id %% aggregate_root_type %%',
                                 NEW.aggregate_root_id, NEW.aggregate_root_type;
                              END IF;
                        
                              SELECT version
                                INTO last_version
                                FROM %1$s.event
                                WHERE aggregate_root_id = NEW.aggregate_root_id
                                  AND aggregate_root_type = NEW.aggregate_root_type
                                ORDER BY version DESC
                                LIMIT 1;
                        
                              expected_version = last_version + 1;
                        
                              IF NEW.version != expected_version THEN
                                RAISE EXCEPTION 'current version unexpected %% - expected version %%',
                                  NEW.version, expected_version;
                              END IF;
                        
                              RETURN NEW;
                            END;
                            $_$;
                          END IF;
                        
                          BEGIN
                            CREATE TRIGGER event_check_version_on_create_trigger
                              BEFORE INSERT ON %1$s.event
                              FOR EACH ROW
                              EXECUTE FUNCTION %1$s.event_check_version_on_create();
                          EXCEPTION
                            WHEN duplicate_object THEN
                              RAISE NOTICE 'Trigger event_check_version_on_create_trigger EXISTS';
                          END;
                        
                          --------------------------------------------------------------------------
                          -- event_immutable
                          --------------------------------------------------------------------------
                          IF EXISTS (
                            SELECT 1 FROM information_schema.routines
                            WHERE routine_name = 'event_immutable'
                              AND routine_schema = '%1$s'
                          ) THEN
                            RAISE NOTICE 'Routine event_immutable EXISTS';
                          ELSE
                            CREATE FUNCTION %1$s.event_immutable() RETURNS trigger
                              LANGUAGE plpgsql AS $_$
                            BEGIN
                              RAISE EXCEPTION 'not allowed';
                            END;
                            $_$;
                          END IF;
                        
                          BEGIN
                            CREATE TRIGGER event_immutable_trigger
                              BEFORE UPDATE ON %1$s.event
                              FOR EACH ROW
                              EXECUTE FUNCTION %1$s.event_immutable();
                          EXCEPTION
                            WHEN duplicate_object THEN
                              RAISE NOTICE 'Trigger event_immutable_trigger EXISTS';
                          END;
                        
                          --------------------------------------------------------------------------
                          -- event_not_deletable
                          --------------------------------------------------------------------------
                          IF EXISTS (
                            SELECT 1 FROM information_schema.routines
                            WHERE routine_name = 'event_not_deletable'
                              AND routine_schema = '%1$s'
                          ) THEN
                            RAISE NOTICE 'Routine event_not_deletable EXISTS';
                          ELSE
                            CREATE FUNCTION %1$s.event_not_deletable() RETURNS trigger
                              LANGUAGE plpgsql AS $_$
                            BEGIN
                              RAISE EXCEPTION 'not allowed';
                            END;
                            $_$;
                          END IF;
                        
                          BEGIN
                            CREATE TRIGGER event_not_deletable_trigger
                              BEFORE DELETE ON %1$s.event
                              FOR EACH ROW
                              EXECUTE FUNCTION %1$s.event_not_deletable();
                          EXCEPTION
                            WHEN duplicate_object THEN
                              RAISE NOTICE 'Trigger event_not_deletable_trigger EXISTS';
                          END;
                        
                          BEGIN
                            CREATE TRIGGER event_table_not_truncable_trigger
                              BEFORE TRUNCATE ON %1$s.event
                              EXECUTE FUNCTION %1$s.event_not_deletable();
                          EXCEPTION
                            WHEN duplicate_object THEN
                              RAISE NOTICE 'Trigger event_table_not_truncable_trigger EXISTS';
                          END;
                        END;
                        $MAIN$;
                        """.formatted(schemaName);
        additionalVolumeBuildItemBuildProducer.produce(new AdditionalVolumeBuildItem(
                new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME),
                new ComposeServiceBuildItem.Volume("./%s_aggregates.sql".formatted(schemaName), "/docker-entrypoint-initdb.d/%s_aggregates.sql".formatted(schemaName),
                        aggregates.getBytes(StandardCharsets.UTF_8))));
    }
}

package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.core.AggregateIdGenerator;
import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.command.JvmCommandHandlerRegistry;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import com.damdamdeo.pulse.extension.writer.deployment.items.IdentifiableBuildItem;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultInstantProvider;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultQuarkusTransaction;
import com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresSequenceGenerator;
import com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresDistributedLockManager;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
                                JdbcPostgresSequenceGenerator.class,
                                JdbcPostgresDistributedLockManager.class)
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
        final String sequenceByAggregateRootTypeAndBelongsTo =
                // language=sql
                """
                        CREATE SCHEMA IF NOT EXISTS %1$s;
                        
                        DO $MAIN$
                        BEGIN
                        
                          CREATE TABLE IF NOT EXISTS %1$s.sequences (
                            owned_by character varying(255) not null,
                            identifiable_clazz character varying(255) not null,
                            belongs_to character varying(255) not null,
                            next_value bigint not null check (next_value > 0),
                            CONSTRAINT identifiable_clazz_and_belongs_to_pkey PRIMARY KEY (identifiable_clazz, belongs_to)
                          );
                        
                          --------------------------------------------------------------------------
                          -- check_owner
                          --------------------------------------------------------------------------
                          CREATE OR REPLACE FUNCTION %1$s.check_owner()
                          RETURNS trigger AS $_$
                          BEGIN
                            IF OLD.owned_by <> NEW.owned_by THEN
                              RAISE EXCEPTION 'Sequence already owned by %%', OLD.owned_by USING ERRCODE = 'P0001';
                            END IF;
                            RETURN NEW;
                          END;
                          $_$ LANGUAGE plpgsql;
                          
                          CREATE TRIGGER check_owner_trigger
                          BEFORE UPDATE ON %1$s.sequences
                          FOR EACH ROW
                          EXECUTE FUNCTION %1$s.check_owner();
                          
                          --------------------------------------------------------------------------
                          -- check_next_value_increment
                          --------------------------------------------------------------------------
                          CREATE OR REPLACE FUNCTION %1$s.check_next_value_increment()
                          RETURNS trigger AS
                          $_$
                          BEGIN
                            IF NEW.next_value <> OLD.next_value + 1 THEN
                              RAISE EXCEPTION
                                'next_value must be incremented by exactly 1 (old=%%, new=%%)',
                                OLD.next_value,
                                NEW.next_value
                                USING ERRCODE = 'P0001';
                            END IF;
                          
                            RETURN NEW;
                          END;
                          $_$ LANGUAGE plpgsql;
                          
                          CREATE TRIGGER check_next_value_increment_trigger
                          BEFORE UPDATE OF next_value
                          ON %1$s.sequences
                          FOR EACH ROW
                          EXECUTE FUNCTION %1$s.check_next_value_increment();
                          
                          --------------------------------------------------------------------------
                          -- prevent_delete
                          --------------------------------------------------------------------------
                          CREATE OR REPLACE FUNCTION %1$s.prevent_delete()
                          RETURNS trigger AS
                          $_$
                          BEGIN
                            RAISE EXCEPTION
                              'Deletion of sequences is not allowed (identifiable_clazz=%%, belongs_to=%%).',
                              OLD.identifiable_clazz,
                              OLD.belongs_to
                              USING ERRCODE = 'P0001';
                          END;
                          $_$ LANGUAGE plpgsql;
                          
                          CREATE TRIGGER prevent_delete_trigger
                          BEFORE DELETE
                          ON %1$s.sequences
                          FOR EACH ROW
                          EXECUTE FUNCTION %1$s.prevent_delete();
                        END;
                        $MAIN$;
                        """.formatted("pulse");
        additionalVolumeBuildItemBuildProducer.produce(new AdditionalVolumeBuildItem(
                new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME),
                new ComposeServiceBuildItem.Volume("./%s_sequences_tables.sql".formatted("pulse"), "/docker-entrypoint-initdb.d/%s_sequences_tables.sql".formatted("pulse"),
                        sequenceByAggregateRootTypeAndBelongsTo.getBytes(StandardCharsets.UTF_8), "sql")));
        final String schemaName = SchemaName.from(new ApplicationNaming(applicationInfoBuildItem.getName())).name();
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
                            CONSTRAINT executed_by_format_chk CHECK (executed_by = 'A' OR executed_by LIKE 'EU:%%' OR executed_by LIKE 'SA:%%' OR executed_by = 'NA' OR executed_by LIKE 'JOB:%%')
                          ) PARTITION BY HASH (aggregate_root_type, aggregate_root_id);
                        
                          CREATE TABLE IF NOT EXISTS %1$s.event_00 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 0);
                          CREATE TABLE IF NOT EXISTS %1$s.event_01 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 1);
                          CREATE TABLE IF NOT EXISTS %1$s.event_02 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 2);
                          CREATE TABLE IF NOT EXISTS %1$s.event_03 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 3);
                          CREATE TABLE IF NOT EXISTS %1$s.event_04 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 4);
                          CREATE TABLE IF NOT EXISTS %1$s.event_05 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 5);
                          CREATE TABLE IF NOT EXISTS %1$s.event_06 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 6);
                          CREATE TABLE IF NOT EXISTS %1$s.event_07 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 7);
                          CREATE TABLE IF NOT EXISTS %1$s.event_08 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 8);
                          CREATE TABLE IF NOT EXISTS %1$s.event_09 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 9);
                          CREATE TABLE IF NOT EXISTS %1$s.event_10 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 10);
                          CREATE TABLE IF NOT EXISTS %1$s.event_11 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 11);
                          CREATE TABLE IF NOT EXISTS %1$s.event_12 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 12);
                          CREATE TABLE IF NOT EXISTS %1$s.event_13 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 13);
                          CREATE TABLE IF NOT EXISTS %1$s.event_14 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 14);
                          CREATE TABLE IF NOT EXISTS %1$s.event_15 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 15);
                          CREATE TABLE IF NOT EXISTS %1$s.event_16 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 16);
                          CREATE TABLE IF NOT EXISTS %1$s.event_17 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 17);
                          CREATE TABLE IF NOT EXISTS %1$s.event_18 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 18);
                          CREATE TABLE IF NOT EXISTS %1$s.event_19 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 19);
                          CREATE TABLE IF NOT EXISTS %1$s.event_20 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 20);
                          CREATE TABLE IF NOT EXISTS %1$s.event_21 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 21);
                          CREATE TABLE IF NOT EXISTS %1$s.event_22 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 22);
                          CREATE TABLE IF NOT EXISTS %1$s.event_23 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 23);
                          CREATE TABLE IF NOT EXISTS %1$s.event_24 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 24);
                          CREATE TABLE IF NOT EXISTS %1$s.event_25 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 25);
                          CREATE TABLE IF NOT EXISTS %1$s.event_26 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 26);
                          CREATE TABLE IF NOT EXISTS %1$s.event_27 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 27);
                          CREATE TABLE IF NOT EXISTS %1$s.event_28 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 28);
                          CREATE TABLE IF NOT EXISTS %1$s.event_29 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 29);
                          CREATE TABLE IF NOT EXISTS %1$s.event_30 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 30);
                          CREATE TABLE IF NOT EXISTS %1$s.event_31 PARTITION OF %1$s.event FOR VALUES WITH (MODULUS 32, REMAINDER 31);
                        
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
                        aggregates.getBytes(StandardCharsets.UTF_8), "sql")));
    }
}

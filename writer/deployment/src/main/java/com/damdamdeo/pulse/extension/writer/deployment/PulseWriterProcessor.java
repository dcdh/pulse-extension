package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.PulseCommonProcessor;
import com.damdamdeo.pulse.extension.common.deployment.items.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.core.command.JvmCommandHandlerRegistry;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultInstantProvider;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultQuarkusTransaction;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
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
                        .addBeanClasses(DefaultQuarkusTransaction.class, DefaultInstantProvider.class)
                        .build(),
                // TODO it is not possible to define the bean with @DefaultBean
                // Should conditionally add it if no other implementation is present.
                AdditionalBeanBuildItem.builder()
                        .addBeanClass(JvmCommandHandlerRegistry.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .build()
        );
    }

    @BuildStep
    ComposeServiceBuildItem generateCompose() {
        return PulseCommonProcessor.POSTGRES_COMPOSE_SERVICE_BUILD_ITEM;
    }

    @BuildStep
    AdditionalVolumeBuildItem additionalVolumeBuildItem(final ApplicationInfoBuildItem applicationInfoBuildItem) {
        final String schemaName = applicationInfoBuildItem.getName().toLowerCase();
        return new AdditionalVolumeBuildItem(
                PulseCommonProcessor.POSTGRES_SERVICE_NAME,
                new ComposeServiceBuildItem.Volume(
                        "./%s_writer.sql".formatted(schemaName),
                        "/docker-entrypoint-initdb.d/%s_writer.sql".formatted(schemaName),
                        // language=sql
                        """
                                CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
                                CREATE SCHEMA IF NOT EXISTS %1$s;
                                
                                DO $MAIN$
                                BEGIN
                                
                                  CREATE TABLE IF NOT EXISTS %1$s.t_aggregate_root (
                                    aggregate_root_type character varying(255) not null,
                                    aggregate_root_id character varying(255) not null,
                                    last_version bigint not null,
                                    aggregate_root_payload bytea NOT NULL CHECK (octet_length(aggregate_root_payload) <= 1000 * 1024),
                                    owned_by character varying(255) not null,
                                    belongs_to character varying(255) not null,
                                    CONSTRAINT t_aggregate_root_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type)
                                  );
                                
                                  CREATE TABLE IF NOT EXISTS %1$s.t_event (
                                    aggregate_root_type character varying(255) not null,
                                    aggregate_root_id character varying(255) not null,
                                    version bigint not null,
                                    creation_date timestamp(6) without time zone not null,
                                    event_type character varying(255) not null,
                                    event_payload bytea not null CHECK (octet_length(event_payload) <= 1000 * 1024),
                                    owned_by character varying(255) not null,
                                    belongs_to character varying(255) not null,
                                    CONSTRAINT event_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type, version),
                                    CONSTRAINT event_unique UNIQUE (aggregate_root_id, aggregate_root_type, version)
                                  );
                                
                                  CREATE INDEX IF NOT EXISTS idx_t_event_aggregate_root_identifier
                                    ON %1$s.t_event USING BTREE (aggregate_root_id, aggregate_root_type);
                                
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
                                        FROM %1$s.t_event
                                        WHERE aggregate_root_id = NEW.aggregate_root_id
                                          AND aggregate_root_type = NEW.aggregate_root_type;
                                
                                      IF NEW.version = 0 AND event_count > 0 THEN
                                        RAISE EXCEPTION
                                         'Event already present while should not be ! aggregate_root_id %% aggregate_root_type %%',
                                         NEW.aggregate_root_id, NEW.aggregate_root_type;
                                      END IF;
                                
                                      SELECT version
                                        INTO last_version
                                        FROM %1$s.t_event
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
                                      BEFORE INSERT ON %1$s.t_event
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
                                      BEFORE UPDATE ON %1$s.t_event
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
                                      BEFORE DELETE ON %1$s.t_event
                                      FOR EACH ROW
                                      EXECUTE FUNCTION %1$s.event_not_deletable();
                                  EXCEPTION
                                    WHEN duplicate_object THEN
                                      RAISE NOTICE 'Trigger event_not_deletable_trigger EXISTS';
                                  END;
                                
                                  BEGIN
                                    CREATE TRIGGER event_table_not_truncable_trigger
                                      BEFORE TRUNCATE ON %1$s.t_event
                                      EXECUTE FUNCTION %1$s.event_not_deletable();
                                  EXCEPTION
                                    WHEN duplicate_object THEN
                                      RAISE NOTICE 'Trigger event_table_not_truncable_trigger EXISTS';
                                  END;
                                
                                END;
                                $MAIN$;
                                """.formatted(schemaName).getBytes(StandardCharsets.UTF_8))
        );
    }
}

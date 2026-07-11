package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;

import java.nio.charset.StandardCharsets;

public class DatabaseProcessor {

    @BuildStep
    void generateAdditionalVolumeBuildItems(final Capabilities capabilities,
                                            final LaunchModeBuildItem launchModeBuildItem,
                                            final ApplicationInfoBuildItem applicationInfoBuildItem,
                                            final BuildProducer<AdditionalVolumeBuildItem> additionalVolumeBuildItemBuildProducer) {
        if (launchModeBuildItem.getLaunchMode().isDevOrTest() && shouldGenerate(capabilities)) {
            final String schemaName = SchemaName.from(new ApplicationNaming(applicationInfoBuildItem.getName())).name();
            // TODO find a way to to mutualization with write extension
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
                            aggregates.getBytes(StandardCharsets.UTF_8), "sql")));
        }
    }

    public static boolean shouldGenerate(final Capabilities capabilities) {
        return !capabilities.isPresent("com.damdamdeo.pulse-writer-extension");
    }
}

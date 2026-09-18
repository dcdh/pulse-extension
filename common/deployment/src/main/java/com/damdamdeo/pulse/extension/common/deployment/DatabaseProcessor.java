package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

import java.nio.charset.StandardCharsets;

class DatabaseProcessor {

    @BuildStep
    void generateAdditionalVolumeBuildItems(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                            final BuildProducer<AdditionalVolumeBuildItem> additionalVolumeBuildItemBuildProducer) {
        final String schemaName = SchemaName.from(new ApplicationNaming(applicationInfoBuildItem.getName())).name();
        // TODO find another way to do it regarding event - code duplicates
        // language=sql
        final String aggregateExecutedByQuery = """
                CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
                CREATE SCHEMA IF NOT EXISTS %1$s;
                
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
                
                CREATE TABLE IF NOT EXISTS %1$s.aggregate_executed_by (
                    aggregate_root_id character varying(255) not null,
                    executed_by character varying(255) not null,
                    owned_by character varying(255) not null,
                    PRIMARY KEY (aggregate_root_id, executed_by)
                );
                
                --------------------------------------------------------------------------
                -- insert_aggregate_executed_by
                --------------------------------------------------------------------------
                CREATE OR REPLACE FUNCTION %1$s.insert_aggregate_executed_by()
                RETURNS trigger
                LANGUAGE plpgsql
                AS $_$
                BEGIN
                    INSERT INTO %1$s.aggregate_executed_by (
                        aggregate_root_id,
                        executed_by,
                        owned_by
                    )
                    VALUES (
                        NEW.aggregate_root_id,
                        NEW.executed_by,
                        NEW.owned_by
                    )
                    ON CONFLICT (aggregate_root_id, executed_by) DO NOTHING;
                
                    RETURN NEW;
                END;
                $_$;
                
                DROP TRIGGER IF EXISTS trg_insert_aggregate_executed_by ON %1$s.event;
                
                CREATE TRIGGER trg_insert_aggregate_executed_by
                AFTER INSERT
                ON %1$s.event
                FOR EACH ROW
                EXECUTE FUNCTION %1$s.insert_aggregate_executed_by();
                """.formatted(schemaName);
        additionalVolumeBuildItemBuildProducer.produce(new AdditionalVolumeBuildItem(
                new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME),
                new ComposeServiceBuildItem.Volume("./%s_aggregate_executed_by.sql".formatted(schemaName), "/docker-entrypoint-initdb.d/%s_aggregate_executed_by.sql".formatted(schemaName),
                        aggregateExecutedByQuery.getBytes(StandardCharsets.UTF_8), "sql")));
    }
}

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
        // This code will be called before the one in PulseWriterProcessor when running tests
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
                  CONSTRAINT executed_by_format_chk CHECK (executed_by = 'A' OR executed_by LIKE 'EU:%%' OR executed_by LIKE 'SA:%%' OR executed_by = 'NA')
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

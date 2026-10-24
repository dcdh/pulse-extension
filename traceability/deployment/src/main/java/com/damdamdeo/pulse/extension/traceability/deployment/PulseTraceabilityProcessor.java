package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import java.nio.charset.StandardCharsets;

public class PulseTraceabilityProcessor {

    private static final String FEATURE = "pulse-traceability-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void generateAdditionalVolumeBuildItem(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                           final TraceabilityConfiguration traceabilityConfiguration,
                                           final BuildProducer<AdditionalVolumeBuildItem> additionalVolumeBuildItemBuildProducer) {
        final String schemaName = SchemaName.from(new ApplicationNaming(applicationInfoBuildItem.getName())).name();
        final String tablesDefinition = switch (traceabilityConfiguration.tracingMode()) {
            case DISABLED -> null;
            case INVOLVED, INVOLVED_WITH_FULL_DETAILS -> // language=sql
                    """
                            CREATE SCHEMA IF NOT EXISTS %1$s;
                            
                            CREATE TABLE IF NOT EXISTS %1$s.executed_by_encoded (
                              id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
                              executed_by_hashed character varying(255) NOT NULL,
                              executed_by_encoded character varying(255) NOT NULL,
                              CONSTRAINT ebe_pkey PRIMARY KEY (id),
                              CONSTRAINT ebe_unique UNIQUE (executed_by_hashed)
                            );
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate (
                              aggregate_root_id character varying(255) NOT NULL,
                              executed_by_encoded_id bigint NOT NULL,
                              command_nb_of_times bigint DEFAULT 0,
                              command_unauthorized_nb_of_times bigint DEFAULT 0,
                              command_business_failed_nb_of_times bigint DEFAULT 0,
                              query_nb_of_times bigint DEFAULT 0,
                              query_unauthorized_nb_of_times bigint DEFAULT 0,
                              traceability_nb_of_times bigint DEFAULT 0,
                              CONSTRAINT ta_pkey PRIMARY KEY (aggregate_root_id, executed_by_encoded_id),
                              CONSTRAINT ta_ebe_fkey
                                FOREIGN KEY (executed_by_encoded_id)
                                REFERENCES %1$s.executed_by_encoded (id)
                            ) PARTITION BY HASH (aggregate_root_id, executed_by_encoded_id);
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_00 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 0);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_01 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 1);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_02 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 2);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_03 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 3);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_04 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 4);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_05 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 5);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_06 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 6);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_07 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 7);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_08 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 8);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_09 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 9);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_10 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 10);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_11 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 11);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_12 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 12);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_13 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 13);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_14 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 14);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_15 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 15);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_16 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 16);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_17 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 17);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_18 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 18);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_19 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 19);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_20 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 20);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_21 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 21);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_22 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 22);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_23 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 23);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_24 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 24);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_25 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 25);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_26 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 26);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_27 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 27);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_28 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 28);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_29 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 29);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_30 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 30);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_31 PARTITION OF %1$s.traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 31);
                            
                            CREATE SEQUENCE IF NOT EXISTS %1$s.trace_id_seq START WITH 1 INCREMENT BY 1;
                            CREATE SEQUENCE IF NOT EXISTS %1$s.correlation_id_seq START WITH 1 INCREMENT BY 1;
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details (
                              trace_id bigint not null,
                              correlation_id bigint not null,
                              executed_at timestamptz not null,
                              source_value int not null,
                              execution_status int not null,
                              from_value character varying(255) not null,
                              CONSTRAINT td_pkey PRIMARY KEY (trace_id),
                              CONSTRAINT td_source_check CHECK (source_value IN (0, 1, 2)),
                              CONSTRAINT td_execution_status_check CHECK (execution_status IN (0, 1, 2))
                            );
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate (
                                traceability_details_id bigint not null,
                                traceability_aggregate_aggregate_root_id character varying(255) NOT NULL,
                                traceability_aggregate_executed_by_encoded_id bigint NOT NULL,
                                CONSTRAINT tdta_pkey PRIMARY KEY (traceability_details_id, traceability_aggregate_aggregate_root_id, traceability_aggregate_executed_by_encoded_id),
                                CONSTRAINT tdta_td_id_fkey
                                  FOREIGN KEY (traceability_details_id)
                                  REFERENCES %1$s.traceability_details (trace_id),
                                CONSTRAINT tdta_ta_id_fkey
                                  FOREIGN KEY (traceability_aggregate_aggregate_root_id, traceability_aggregate_executed_by_encoded_id)
                                  REFERENCES %1$s.traceability_aggregate (aggregate_root_id, executed_by_encoded_id)
                            ) PARTITION BY HASH (traceability_details_id, traceability_aggregate_aggregate_root_id, traceability_aggregate_executed_by_encoded_id);
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_00 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 0);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_01 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 1);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_02 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 2);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_03 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 3);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_04 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 4);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_05 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 5);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_06 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 6);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_07 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 7);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_08 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 8);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_09 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 9);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_10 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 10);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_11 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 11);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_12 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 12);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_13 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 13);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_14 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 14);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_15 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 15);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_16 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 16);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_17 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 17);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_18 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 18);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_19 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 19);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_20 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 20);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_21 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 21);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_22 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 22);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_23 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 23);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_24 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 24);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_25 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 25);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_26 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 26);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_27 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 27);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_28 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 28);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_29 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 29);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_30 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 30);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate_31 PARTITION OF %1$s.traceability_details_traceability_aggregate FOR VALUES WITH (MODULUS 32, REMAINDER 31);
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events (
                                id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
                                traceability_trace_id bigint NOT NULL,
                                aggregate_root_id varchar(255) NOT NULL,
                                projection_id varchar(255),
                                event_type character varying(255),
                                aggregate_version bigint,
                                CONSTRAINT tae_pkey PRIMARY KEY (aggregate_root_id, id),
                                CONSTRAINT tt_fkey
                                  FOREIGN KEY (traceability_trace_id)
                                  REFERENCES %1$s.traceability_details (trace_id)
                            ) PARTITION BY HASH (aggregate_root_id);
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_00 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 0);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_01 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 1);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_02 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 2);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_03 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 3);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_04 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 4);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_05 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 5);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_06 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 6);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_07 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 7);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_08 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 8);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_09 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 9);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_10 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 10);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_11 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 11);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_12 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 12);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_13 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 13);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_14 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 14);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_15 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 15);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_16 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 16);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_17 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 17);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_18 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 18);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_19 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 19);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_20 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 20);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_21 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 21);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_22 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 22);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_23 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 23);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_24 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 24);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_25 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 25);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_26 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 26);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_27 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 27);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_28 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 28);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_29 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 29);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_30 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 30);
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate_events_31 PARTITION OF %1$s.traceability_aggregate_events FOR VALUES WITH (MODULUS 32, REMAINDER 31);
                            """.formatted(schemaName);
        };
        if (tablesDefinition != null) {
            additionalVolumeBuildItemBuildProducer.produce(new AdditionalVolumeBuildItem(
                    new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME),
                    new ComposeServiceBuildItem.Volume("./%s_traceability_tables.sql".formatted(schemaName), "/docker-entrypoint-initdb.d/%s_traceability_tables.sql".formatted("pulse"),
                            tablesDefinition.getBytes(StandardCharsets.UTF_8), "sql")));
        }
    }
}

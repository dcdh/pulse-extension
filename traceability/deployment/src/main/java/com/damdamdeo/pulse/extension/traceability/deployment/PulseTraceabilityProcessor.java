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
                              CONSTRAINT executed_by_encoded_pkey PRIMARY KEY (id),
                              CONSTRAINT executed_by_encoded_unique UNIQUE (executed_by_hashed)
                            );
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate (
                              id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
                              aggregate_root_id character varying(255) NOT NULL,
                              executed_by_encoded_id bigint NOT NULL,
                              command_nb_of_times bigint DEFAULT 0,
                              query_nb_of_times bigint DEFAULT 0,
                              CONSTRAINT traceability_aggregate_pkey PRIMARY KEY (id),
                              CONSTRAINT traceability_aggregate_unique
                                UNIQUE (aggregate_root_id, executed_by_encoded_id),
                              CONSTRAINT traceability_aggregate_executed_by_encoded_fkey
                                FOREIGN KEY (executed_by_encoded_id)
                                REFERENCES %1$s.executed_by_encoded (id)
                            );
                            
                            CREATE SEQUENCE IF NOT EXISTS %1$s.trace_id_seq START WITH 1 INCREMENT BY 1;
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details (
                              trace_id bigint not null,
                              executed_at timestamptz not null,
                              source_value int not null,
                              from_value character varying(255) not null,
                              CONSTRAINT traceability_details_pkey PRIMARY KEY (trace_id),
                              CONSTRAINT traceability_details_source_check CHECK (source_value IN (0, 1))
                            );
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details_traceability_aggregate (
                                traceability_details_id bigint not null,
                                traceability_aggregate_id bigint not null,
                                CONSTRAINT traceability_details_traceability_aggregate_unique
                                  UNIQUE (traceability_details_id, traceability_aggregate_id),
                                CONSTRAINT traceability_details_traceability_aggregate_traceability_details_id_fkey
                                  FOREIGN KEY (traceability_details_id)
                                  REFERENCES %1$s.traceability_details (trace_id),
                                CONSTRAINT traceability_details_traceability_aggregate_traceability_aggregate_id_fkey
                                  FOREIGN KEY (traceability_aggregate_id)
                                  REFERENCES %1$s.traceability_aggregate (id)
                            );
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

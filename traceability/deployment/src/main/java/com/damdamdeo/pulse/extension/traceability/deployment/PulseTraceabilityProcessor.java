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
            case INVOLVED -> // language=sql
                    """
                            CREATE SCHEMA IF NOT EXISTS %1$s;
                            
                            CREATE TABLE IF NOT EXISTS %1$s.executed_by_encoded (
                              id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
                              executed_by_hashed character varying(255) NOT NULL,
                              executed_by_encoded character varying(255) NOT NULL,
                              CONSTRAINT executed_by_encoded_pkey PRIMARY KEY (id),
                              CONSTRAINT executed_by_encoded_unique
                                UNIQUE (executed_by_hashed, executed_by_encoded)
                            );
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate (
                              aggregate_root_id character varying(255) NOT NULL,
                              executed_by_encoded_id bigint NOT NULL,
                              CONSTRAINT traceability_aggregate_pkey
                                PRIMARY KEY (aggregate_root_id, executed_by_encoded_id),
                              CONSTRAINT traceability_aggregate_executed_by_encoded_fkey
                                FOREIGN KEY (executed_by_encoded_id)
                                REFERENCES %1$s.executed_by_encoded (id)
                            );
                            """.formatted(schemaName);
            case INVOLVED_WITH_FULL_DETAILS -> // language=sql
                    """
                            CREATE SCHEMA IF NOT EXISTS %1$s;
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_details (
                              trace_id bigint not null,
                              executed_at timestamptz not null,
                              from_value character varying(255) not null,
                              CONSTRAINT aggregate_root_pkey PRIMARY KEY (trace_id)
                            );
                            
                            CREATE TABLE IF NOT EXISTS %1$s.executed_by_encoded (
                              id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
                              executed_by_hashed character varying(255) NOT NULL,
                              executed_by_encoded character varying(255) NOT NULL,
                              CONSTRAINT executed_by_encoded_pkey PRIMARY KEY (id),
                              CONSTRAINT executed_by_encoded_unique
                                UNIQUE (executed_by_hashed, executed_by_encoded)
                            );
                            
                            CREATE TABLE IF NOT EXISTS %1$s.traceability_aggregate (
                              trace_id bigint not null,
                              aggregate_root_id character varying(255) NOT NULL,
                              executed_by_encoded_id bigint NOT NULL,
                              CONSTRAINT traceability_aggregate_pkey
                                PRIMARY KEY (aggregate_root_id, executed_by_encoded_id),
                              CONSTRAINT traceability_details_fkey
                                FOREIGN KEY (trace_id)
                                REFERENCES %1$s.traceability_details (trace_id),
                              CONSTRAINT traceability_aggregate_executed_by_encoded_fkey
                                FOREIGN KEY (executed_by_encoded_id)
                                REFERENCES %1$s.executed_by_encoded (id)
                            );
                            """.formatted(schemaName);
        };
        if (tablesDefinition != null) {
            additionalVolumeBuildItemBuildProducer.produce(new AdditionalVolumeBuildItem(
                    new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME),
                    new ComposeServiceBuildItem.Volume("./%s_traceability_tables.sql".formatted("pulse"), "/docker-entrypoint-initdb.d/%s_traceability_tables.sql".formatted("pulse"),
                            tablesDefinition.getBytes(StandardCharsets.UTF_8), "sql")));
        }
    }
}

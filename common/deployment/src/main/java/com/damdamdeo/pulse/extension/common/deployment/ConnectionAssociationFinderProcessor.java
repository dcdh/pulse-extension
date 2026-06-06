package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.datasource.JdbcPostgresConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionAssociationFinder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

import java.nio.charset.StandardCharsets;

public class ConnectionAssociationFinderProcessor {

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(JdbcPostgresConnectionIdentifierRepository.class, ConnectionAssociationFinder.class)
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .setUnremovable()
                .build();
    }

    @BuildStep
    AdditionalVolumeBuildItem generateAdditionalVolumeBuildItem(final ApplicationInfoBuildItem applicationInfoBuildItem) {
        final String schemaName = applicationInfoBuildItem.getName().toLowerCase();
        return new AdditionalVolumeBuildItem(
                new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME),
                new ComposeServiceBuildItem.Volume("./%s_connection_identifier.sql".formatted(schemaName), "/docker-entrypoint-initdb.d/%s_connection_identifier.sql".formatted(schemaName),
                        // language=sql
                        """
                                 CREATE SCHEMA IF NOT EXISTS %1$s;
                                 CREATE TABLE %1$s.connection_identifier (
                                    connection_identifier_hash varchar(64) NOT NULL,
                                    identifiable_id varchar(255) NOT NULL,
                                    CONSTRAINT connection_identifier_hash_unique
                                        UNIQUE (connection_identifier_hash)
                                );
                                """.formatted(schemaName).getBytes(StandardCharsets.UTF_8), "sql"));
    }
}

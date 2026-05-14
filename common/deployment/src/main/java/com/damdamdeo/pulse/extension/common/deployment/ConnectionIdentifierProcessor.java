package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.PostgresSqlScriptBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.datasource.JdbcPostgresConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierAssociation;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public class ConnectionIdentifierProcessor {

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(JdbcPostgresConnectionIdentifierRepository.class, ConnectionIdentifierAssociation.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    PostgresSqlScriptBuildItem generatePostgresSqlScriptBuildItems(final ApplicationInfoBuildItem applicationInfoBuildItem) {
        final String schemaName = applicationInfoBuildItem.getName().toLowerCase();
        return new PostgresSqlScriptBuildItem(
                "%s_common.sql".formatted(schemaName),
                // language=sql
                """
                         CREATE SCHEMA IF NOT EXISTS %1$s;
                         CREATE TABLE %1$s.connection_identifier (
                            connection_identifier_hash varchar(64) NOT NULL,
                            identifiable_id varchar(255) NOT NULL,
                            CONSTRAINT connection_identifier_hash_unique
                                UNIQUE (connection_identifier_hash)
                        );
                        """.formatted(schemaName)
        );
    }
}

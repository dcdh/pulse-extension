package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.runtime.datasource.JdbcPostgresConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionAssociationFinder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class ConnectionAssociationFinderProcessor {

    public static final Supplier<Boolean> hasQuarkusJdbcPostgresInClassPath = () -> QuarkusClassLoader.isClassPresentAtRuntime("io.quarkus.jdbc.postgresql.runtime.PostgreSQLAgroalConnectionConfigurer");

    @BuildStep
    AdditionalBeanBuildItem additionalBeans(final Capabilities capabilities) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (capabilities.isPresent(Capability.OIDC) && hasQuarkusJdbcPostgresInClassPath.get()) {
            builder.addBeanClasses(JdbcPostgresConnectionIdentifierRepository.class, ConnectionAssociationFinder.class)
                    .setDefaultScope(DotNames.APPLICATION_SCOPED)
                    .setUnremovable();
        }
        return builder.build();
    }

    @BuildStep
    void generateAdditionalVolumeBuildItem(final Capabilities capabilities,
                                           final BuildProducer<AdditionalVolumeBuildItem> additionalVolumeBuildItemBuildProducer) {
        if (capabilities.isPresent(Capability.OIDC) && hasQuarkusJdbcPostgresInClassPath.get()) {
            final String schemaName = "pulse";
            additionalVolumeBuildItemBuildProducer.produce(new AdditionalVolumeBuildItem(
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
                                    """.formatted(schemaName).getBytes(StandardCharsets.UTF_8), "sql")));
        }
    }
}

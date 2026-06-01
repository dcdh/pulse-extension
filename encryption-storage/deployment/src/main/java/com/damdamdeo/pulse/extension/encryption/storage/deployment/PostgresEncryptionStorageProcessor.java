package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeProcessor;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.Dependency;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils.SERVICE_NAME;

public class PostgresEncryptionStorageProcessor {

    public static final Dependency QUARKUS_JDBC_POSTGRESQL_DEPENDENCY = Dependency.of("io.quarkus", "quarkus-jdbc-postgresql");

    @BuildStep
    List<AdditionalVolumeBuildItem> generatePassphraseTable(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                                            final CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (EncryptionStorageProcessor.hasDependency(curateOutcomeBuildItem, QUARKUS_JDBC_POSTGRESQL_DEPENDENCY)) {
            final String schemaName = applicationInfoBuildItem.getName().toLowerCase();
            return List.of(new AdditionalVolumeBuildItem(
                    new ComposeServiceBuildItem.ServiceName(SERVICE_NAME),
                    new ComposeServiceBuildItem.Volume("./%s_passphrase.sql".formatted(schemaName), "/docker-entrypoint-initdb.d/%s_passphrase.sql".formatted(schemaName),
                            // language=sql
                            """
                                    CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
                                    CREATE SCHEMA IF NOT EXISTS %1$s;
                                    CREATE TABLE %1$s.passphrase (
                                        owned_by_hashed VARCHAR(255) PRIMARY KEY,
                                        passphrase bytea NOT NULL                                        
                                    );
                                    """.formatted(schemaName).getBytes(StandardCharsets.UTF_8))
            ));
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<ComposeServiceBuildItem> generateCompose(final CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (EncryptionStorageProcessor.hasDependency(curateOutcomeBuildItem, QUARKUS_JDBC_POSTGRESQL_DEPENDENCY)) {
            return List.of(ComposeProcessor.POSTGRES_COMPOSE_SERVICE_BUILD_ITEM);
        } else {
            return List.of();
        }
    }
}

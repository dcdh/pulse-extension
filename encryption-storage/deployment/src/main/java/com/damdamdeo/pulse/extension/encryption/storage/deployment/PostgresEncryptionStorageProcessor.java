package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeProcessor;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils.SERVICE_NAME;
import static com.damdamdeo.pulse.extension.encryption.storage.deployment.EncryptionStorageProcessor.hasQuarkusJdbcPostgresInClassPath;
import static com.damdamdeo.pulse.extension.encryption.storage.deployment.EncryptionStorageProcessor.hasVaultInClassPath;

public class PostgresEncryptionStorageProcessor {

    @BuildStep
    List<AdditionalVolumeBuildItem> generatePassphraseTable() {
        if (match()) {
            final String schemaName = "pulse";
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
                                    """.formatted(schemaName).getBytes(StandardCharsets.UTF_8), "sql")
            ));
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<ComposeServiceBuildItem> generateCompose() {
        if (match()) {
            return List.of(ComposeProcessor.POSTGRES_COMPOSE_SERVICE_BUILD_ITEM);
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<ContentBuildItem> generateContentBuildItems() {
        if (match()) {
            return List.of(
                    new ContentBuildItem(new Title(Title.Level.SECOND, "PostgreSQL Encryption storage")),
                    new ContentBuildItem(CodeBlock.fromProperties(Map.of("pulse.encryption-storage", "32 characters master key"))));
        } else {
            return List.of();
        }
    }

    private boolean match() {
        return !hasVaultInClassPath.get() && hasQuarkusJdbcPostgresInClassPath.get();
    }
}

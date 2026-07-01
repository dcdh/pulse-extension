package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.common.runtime.connectionidentifier.CachedConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.common.runtime.connectionidentifier.JdbcPostgresConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ConnectionIdentifierProcessor {

    public static final Supplier<Boolean> hasQuarkusJdbcPostgresInClassPath = () -> QuarkusClassLoader.isClassPresentAtRuntime("io.quarkus.jdbc.postgresql.runtime.PostgreSQLAgroalConnectionConfigurer");

    @BuildStep
    List<AdditionalBeanBuildItem> produceAdditionalBeanBuildItem(final Capabilities capabilities) {
        final List<AdditionalBeanBuildItem> additionalBeanBuildItems = new ArrayList<>();
        if (capabilities.isPresent(Capability.OIDC) && hasQuarkusJdbcPostgresInClassPath.get()) {
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(ConnectionIdentifierProvider.class)
                    .setDefaultScope(DotNames.APPLICATION_SCOPED)
                    .setUnremovable()
                    .build());
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(JdbcPostgresConnectionIdentifierRepository.class)
                    .build());
            if (capabilities.isPresent(Capability.CACHE)) {
                additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                        .addBeanClasses(CachedConnectionIdentifierRepository.class)
                        .build());
            }
        }
        return additionalBeanBuildItems;
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem additionalIndexedClassesBuildItem(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.OIDC) && hasQuarkusJdbcPostgresInClassPath.get()
                && capabilities.isPresent(Capability.CACHE)) {
            return new AdditionalIndexedClassesBuildItem(CachedConnectionIdentifierRepository.class.getName());
        } else {
            return new AdditionalIndexedClassesBuildItem();
        }
    }

    private static final Map<String, String> CACHING_CONFIGURATIONS = Map.of(
            "quarkus.cache.caffeine.\"connectionIdentifier\".initial-capacity", "10000",
            "quarkus.cache.caffeine.\"connectionIdentifier\".maximum-size", "10000",
            "quarkus.cache.caffeine.\"connectionIdentifier\".expire-after-write", "7D");

    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> defineDefaultConfiguration(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return CACHING_CONFIGURATIONS.entrySet().stream()
                    .map(e -> new RunTimeConfigurationDefaultBuildItem(e.getKey(), e.getValue()))
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<ContentBuildItem> contentBuildItems(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            final List<ContentBuildItem> contentBuildItems = new ArrayList<>();
            contentBuildItems.add(new ContentBuildItem(new Title(Title.Level.SECOND, "ConnectionIdentifier caching configuration")));
            contentBuildItems.add(new ContentBuildItem(CodeBlock.fromProperties(CACHING_CONFIGURATIONS)));
            return contentBuildItems;
        } else {
            return List.of();
        }
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

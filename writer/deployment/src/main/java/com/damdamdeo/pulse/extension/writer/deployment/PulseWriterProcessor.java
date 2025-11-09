package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.command.JvmCommandHandlerRegistry;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultInstantProvider;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultQuarkusTransaction;
import com.damdamdeo.pulse.extension.writer.runtime.PostgresqlEventStoreInitializer;
import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresqlSchemaInitializer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PulseWriterProcessor {

    private static final String DOCKER_COMPOSE_FILE = "../compose-devservices-pulse-writer.yml";

    private static final String FEATURE = "pulse-writer-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        return List.of(
                AdditionalBeanBuildItem.builder()
                        .addBeanClasses(DefaultQuarkusTransaction.class, DefaultInstantProvider.class)
                        .addBeanClass(PostgresqlEventStoreInitializer.class)
                        .addBeanClass(PostgresqlSchemaInitializer.class)
                        .build(),
                // TODO it is not possible to define the bean with @DefaultBean
                // Should conditionally add it if no other implementation is present.
                AdditionalBeanBuildItem.builder()
                        .addBeanClass(JvmCommandHandlerRegistry.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .build()
        );
    }

    @BuildStep
    void generateCompose(final OutputTargetBuildItem outputTargetBuildItem,
                         // use the GeneratedResourceBuildItem only to ensure that the file will be created before compose is started
                         final BuildProducer<GeneratedResourceBuildItem> generatedResourceBuildItemBuildProducer) throws IOException {
        // language=yaml
        final String composeContent = """
                services:
                  postgres:
                    image: postgres:17.6-alpine3.22
                    labels:
                      io.quarkus.devservices.compose.wait_for.logs: .*database system is ready to accept connections.*
                    restart: always
                    healthcheck:
                      test: 'pg_isready'
                      interval: 10s
                      timeout: 5s
                      retries: 5
                    ports:
                      - "5432"
                    environment:
                      POSTGRES_USER: quarkus
                      POSTGRES_DB: quarkus
                      POSTGRES_PASSWORD: quarkus
                    command: |
                      postgres
                      -c wal_level=logical
                      -c hot_standby=on
                      -c max_wal_senders=10
                      -c max_replication_slots=10
                      -c synchronized_standby_slots=replication_slot
                """;
        final Path resolved = outputTargetBuildItem.getOutputDirectory().resolve(DOCKER_COMPOSE_FILE);
        Files.createDirectories(resolved.getParent());
        Files.writeString(resolved, composeContent, StandardCharsets.UTF_8);
    }
}

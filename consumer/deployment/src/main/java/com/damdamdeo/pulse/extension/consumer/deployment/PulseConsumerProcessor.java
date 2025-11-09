package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.consumer.deployment.items.TargetBuildItem;
import com.damdamdeo.pulse.extension.consumer.runtime.*;
import com.damdamdeo.pulse.extension.core.consumer.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.consumer.SequentialEventChecker;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class PulseConsumerProcessor {

    private static final String DOCKER_COMPOSE_FILE = "../compose-devservices-pulse-consumer.yml";

    @BuildStep
    List<TargetBuildItem> discoverTargets(final List<ValidationErrorBuildItem> validationErrorBuildItems,
                                          final CombinedIndexBuildItem combinedIndexBuildItem) {
        if (validationErrorBuildItems.isEmpty()) {
            final IndexView computingIndex = combinedIndexBuildItem.getComputingIndex();
            return computingIndex.getAnnotations(EventChannel.class)
                    .stream()
                    .map(annotationInstance -> {
                        final Target target = new Target(annotationInstance.value("target").asString());
                        final List<ApplicationNaming> sources = annotationInstance.value("sources").asArrayList().stream()
                                .map(annotationValue -> {
                                    final AnnotationInstance nested = annotationValue.asNested();
                                    return ApplicationNaming.of(
                                            nested.value("functionalDomain").asString(),
                                            nested.value("componentName").asString());
                                }).toList();
                        return new TargetBuildItem(target, sources);
                    })
                    .distinct()
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans(final List<ValidationErrorBuildItem> validationErrorBuildItems) {
        if (validationErrorBuildItems.isEmpty()) {
            return Stream.of(
                            EventChannel.class,
                            JdbcPostgresIdempotencyRepository.class,
                            PostgresAggregateRootLoader.class,
                            JacksonDecryptedPayloadToPayloadMapper.class,
                            DefaultAsyncEventChannelMessageHandlerProvider.class,
                            JsonNodeTargetEventChannelExecutor.class)
                    .map(beanClazz -> AdditionalBeanBuildItem.builder().addBeanClass(beanClazz).build())
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    AdditionalBeanBuildItem registerSequentialEventChecker() {
        return AdditionalBeanBuildItem.builder().addBeanClass(SequentialEventChecker.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build();
    }

    @BuildStep
    void generateCompose(final Capabilities capabilities,
                         final OutputTargetBuildItem outputTargetBuildItem,
                         // use the GeneratedResourceBuildItem only to ensure that the file will be created before compose is started
                         final BuildProducer<GeneratedResourceBuildItem> generatedResourceBuildItemBuildProducer) throws IOException {
        if (!capabilities.isPresent("com.damdamdeo.pulse-writer-extension")
                && !capabilities.isPresent("com.damdamdeo.pulse-publisher-extension")) {
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
                      kafka:
                        image: debezium-for-dev-service/kafka:3.3.1.Final
                        labels:
                          io.quarkus.devservices.compose.exposed_ports: /tmp/ports
                        ports:
                          - "9092"
                          - "29092"
                        environment:
                          - CLUSTER_ID=oh-sxaDRTcyAr6pFRbXyzA
                          - NODE_ID=1
                          - KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
                        healthcheck:
                          test: ["CMD", "./bin/kafka-topics.sh", "--bootstrap-server", "kafka:29092", "--list"]
                          interval: 10s
                          timeout: 5s
                          retries: 5
                          start_period: 10s
                    """;
            final Path resolved = outputTargetBuildItem.getOutputDirectory().resolve(DOCKER_COMPOSE_FILE);
            Files.createDirectories(resolved.getParent());
            Files.writeString(resolved, composeContent, StandardCharsets.UTF_8);
        }
    }
}

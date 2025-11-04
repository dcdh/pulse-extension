package com.damdamdeo.pulse.extension.deployment;

import com.damdamdeo.pulse.extension.runtime.consumer.debezium.*;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class DebeziumProcessor {

    private static final String DOCKER_COMPOSE_FILE = "../compose-devservices-pulse.yml";

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.KAFKA)) {
            return Stream.of(DebeziumConfigurator.class, KafkaConnectorApiExecutor.class,
                            ApplicationNamingProvider.class, ConnectorNamingProvider.class,
                            KafkaConnectorConfigurationGenerator.class)
                    .map(beanClazz -> AdditionalBeanBuildItem.builder().addBeanClass(beanClazz).build())
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalIndexedClassesBuildItem> additionalIndexedClasses(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.KAFKA)) {
            return Stream.of(KafkaConnectorApi.class)
                    .map(Class::getName)
                    .map(AdditionalIndexedClassesBuildItem::new)
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    @Produce(ArtifactResultBuildItem.class)
    void generateCompose(final Capabilities capabilities,
                         final OutputTargetBuildItem outputTargetBuildItem,
                         // use the GeneratedResourceBuildItem only to ensure that the file will be created before compose is started
                         final BuildProducer<GeneratedResourceBuildItem> generatedResourceBuildItemBuildProducer) throws IOException {
        if (capabilities.isPresent(Capability.KAFKA)) {
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
                          POSTGRES_PASSWORD: password
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
                    
                      connect:
                        image: quay.io/debezium/connect:3.3.1.Final
                        labels:
                          io.quarkus.devservices.compose.config_map.port.8083: pulse.debezium.connect.port
                        ports:
                          - "8083"
                        links:
                          - kafka
                          - postgres
                        environment:
                          - BOOTSTRAP_SERVERS=kafka:29092
                          - GROUP_ID=1
                          - CONFIG_STORAGE_TOPIC=my_connect_configs
                          - OFFSET_STORAGE_TOPIC=my_connect_offsets
                          - STATUS_STORAGE_TOPIC=my_connect_statuses
                        healthcheck:
                          test: ["CMD", "curl", "-f", "http://localhost:8083/connectors"]
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

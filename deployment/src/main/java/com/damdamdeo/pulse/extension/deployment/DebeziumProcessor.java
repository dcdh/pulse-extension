package com.damdamdeo.pulse.extension.deployment;

import com.damdamdeo.pulse.extension.runtime.consumer.debezium.*;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

import java.util.List;
import java.util.stream.Stream;

public class DebeziumProcessor {

    private static final String DOCKER_COMPOSE_FILE = "compose-devservices-pulse.yml";

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

//    @BuildStep
//    void toto(final Capabilities capabilities,
//              final BuildProducer<GeneratedFileSystemResourceBuildItem> resources) {
//        if (capabilities.isPresent(Capability.KAFKA)) {
//            // language=yaml
//            final String content = """
//                    services:
//                      postgres:
//                        image: postgres:17-alpine
//                        labels:
//                          io.quarkus.devservices.compose.wait_for.logs: .*database system is ready to accept connections.*
//                        restart: always
//                        healthcheck:
//                          test: 'pg_isready'
//                          interval: 10s
//                          timeout: 5s
//                          retries: 5
//                        ports:
//                          - 5432
//                        environment:
//                          POSTGRES_USER: quarkus
//                          POSTGRES_DB: quarkus
//                          POSTGRES_PASSWORD: password
//                        command: |
//                          postgres\s
//                          -c wal_level=logical\s
//                          -c hot_standby=on\s
//                          -c max_wal_senders=10\s
//                          -c max_replication_slots=10
//                          -c synchronized_standby_slots=replication_slot
//                    """;
//            // TODO kafka native + Kafka connect et je suis bon ! lol c'est good !!!
//
//            resources.produce(new GeneratedFileSystemResourceBuildItem("../"+DOCKER_COMPOSE_FILE, content.getBytes(StandardCharsets.UTF_8)));
//        }
//    }
}

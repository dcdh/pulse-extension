package com.damdamdeo.pulse.extension.publisher.deployment;

import com.damdamdeo.pulse.extension.common.deployment.PulseCommonProcessor;
import com.damdamdeo.pulse.extension.common.deployment.items.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.publisher.runtime.debezium.*;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class PulsePublisherProcessor {

    private static final String FEATURE = "pulse-publisher-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        return Stream.of(DebeziumConfigurator.class, KafkaConnectorApiExecutor.class,
                        FromApplicationProvider.class, ConnectorNamingProvider.class,
                        KafkaConnectorConfigurationGenerator.class)
                .map(beanClazz -> AdditionalBeanBuildItem.builder().addBeanClass(beanClazz).build())
                .toList();
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem additionalIndexedClasses() {
        return new AdditionalIndexedClassesBuildItem(KafkaConnectorApi.class.getName());
    }

    @BuildStep
    List<ComposeServiceBuildItem> generateCompose() {
        return List.of(
                PulseCommonProcessor.KAFKA_COMPOSE_SERVICE_BUILD_ITEM,
                new ComposeServiceBuildItem(
                        new ComposeServiceBuildItem.ServiceName("connect"),
                        new ComposeServiceBuildItem.ImageName("quay.io/debezium/connect:3.3.1.Final"),
                        new ComposeServiceBuildItem.Labels(
                                Map.of("io.quarkus.devservices.compose.config_map.port.8083", "pulse.debezium.connect.port")),
                        new ComposeServiceBuildItem.Ports(List.of("8083")),
                        ComposeServiceBuildItem.Links.on(List.of(
                                PulseCommonProcessor.KAFKA_SERVICE_NAME,
                                PulseCommonProcessor.POSTGRES_SERVICE_NAME)),
                        new ComposeServiceBuildItem.EnvironmentVariables(
                                Map.of("BOOTSTRAP_SERVERS", "kafka:29092",
                                        "GROUP_ID", "1",
                                        "CONFIG_STORAGE_TOPIC", "my_connect_configs",
                                        "OFFSET_STORAGE_TOPIC", "my_connect_offsets",
                                        "STATUS_STORAGE_TOPIC", "my_connect_statuses")),
                        ComposeServiceBuildItem.Command.ofNone(),
                        ComposeServiceBuildItem.Entrypoint.ofNone(),
                        new ComposeServiceBuildItem.HealthCheck(
                                List.of("CMD", "curl", "-f", "http://localhost:8083/connectors"),
                                new ComposeServiceBuildItem.Interval(10),
                                new ComposeServiceBuildItem.Timeout(5),
                                new ComposeServiceBuildItem.Retries(5),
                                new ComposeServiceBuildItem.StartPeriod(10)),
                        List.of(),
                        ComposeServiceBuildItem.DependsOn.on(List.of(
                                PulseCommonProcessor.POSTGRES_SERVICE_NAME,
                                PulseCommonProcessor.KAFKA_SERVICE_NAME)))
        );
    }
}

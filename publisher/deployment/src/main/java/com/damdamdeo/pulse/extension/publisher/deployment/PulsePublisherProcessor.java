package com.damdamdeo.pulse.extension.publisher.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Admonition;
import com.damdamdeo.pulse.extension.build.report.deployment.content.AdmonitionType;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeProcessor;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.kafka.deployment.KafkaProcessor;
import com.damdamdeo.pulse.extension.publisher.runtime.debezium.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
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
                        KafkaConnectorConfigurationGenerator.class, PartitionChecker.class)
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
                KafkaProcessor.KAFKA_COMPOSE_SERVICE_BUILD_ITEM,
                new ComposeServiceBuildItem(
                        new ComposeServiceBuildItem.ServiceName("connect"),
                        new ComposeServiceBuildItem.ImageName("quay.io/debezium/connect:3.5.0.Final"),
                        new ComposeServiceBuildItem.Labels(
                                Map.of("io.quarkus.devservices.compose.config_map.port.8083", "pulse.debezium.connect.port")),
                        new ComposeServiceBuildItem.Ports(List.of("8083")),
                        ComposeServiceBuildItem.Links.on(List.of(
                                KafkaProcessor.KAFKA_SERVICE_NAME,
                                ComposeProcessor.POSTGRES_SERVICE_NAME)),
                        new ComposeServiceBuildItem.EnvironmentVariables(
                                Map.of("BOOTSTRAP_SERVERS", "kafka:29092",
                                        "GROUP_ID", "1",
                                        "CONFIG_STORAGE_TOPIC", "my_connect_configs",
                                        "OFFSET_STORAGE_TOPIC", "my_connect_offsets",
                                        "STATUS_STORAGE_TOPIC", "my_connect_statuses",
                                        "LOG_LEVEL", "INFO")),
                        ComposeServiceBuildItem.Command.ofNone(),
                        ComposeServiceBuildItem.Entrypoint.ofNone(),
                        new ComposeServiceBuildItem.HealthCheck(
                                List.of("CMD", "curl", "-f", "http://localhost:8083/connectors"),
                                new ComposeServiceBuildItem.Interval(10),
                                new ComposeServiceBuildItem.Timeout(5),
                                new ComposeServiceBuildItem.Retries(30),
                                new ComposeServiceBuildItem.StartPeriod(10)),
                        List.of(),
                        ComposeServiceBuildItem.DependsOn.on(List.of(
                                ComposeProcessor.POSTGRES_SERVICE_NAME,
                                KafkaProcessor.KAFKA_SERVICE_NAME)))
        );
    }

    @BuildStep
    List<ContentBuildItem> contentBuildItems(final ApplicationInfoBuildItem applicationInfoBuildItem) throws JsonProcessingException {
        return List.of(
                new ContentBuildItem(new Title(2, "Publisher Debezium configuration")),
                new ContentBuildItem(CodeBlock.fromJson(
                        new ObjectMapper().writeValueAsString(
                                KafkaConnectorConfigurationGenerator.generate(
                                        FromApplication.from(applicationInfoBuildItem.getName()), "localhost", 8083, "datasourceUsername", "datasourcePassword", "database", 1)))),
                new ContentBuildItem(new Admonition(AdmonitionType.NOTE, "host, port, datasourceUsername, datasourcePassword, database, topicCreationDefaultPartitions are determined at runtime"))
        );
    }
}

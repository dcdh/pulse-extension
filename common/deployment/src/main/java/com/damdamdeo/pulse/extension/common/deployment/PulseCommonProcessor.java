package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseGenerator;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseProvider;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPDecryptionService;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.common.runtime.serialization.PulseObjectMapperCustomizer;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.security.deployment.BouncyCastleProviderBuildItem;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PulseCommonProcessor {

    private static Logger LOG = Logger.getLogger(PulseCommonProcessor.class);

    private static final String FEATURE = "pulse-common-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        final List<AdditionalBeanBuildItem> additionalBeanBuildItems = new ArrayList<>();
        additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        PulseObjectMapperCustomizer.class,
                        DefaultPassphraseGenerator.class,
                        DefaultPassphraseProvider.class,
                        OpenPGPDecryptionService.class,
                        OpenPGPEncryptionService.class)
                .build());
        return additionalBeanBuildItems;
    }

    @BuildStep
    void validateApplicationNaming(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                   final BuildProducer<ValidationErrorBuildItem> validationErrorBuildItemProducer) {
        if (!FromApplication.FULL_PATTERN.matcher(applicationInfoBuildItem.getName()).matches()) {
            validationErrorBuildItemProducer.produce(new ValidationErrorBuildItem(
                    new IllegalArgumentException(
                            "Invalid application name '%s' - it should match '%s'".formatted(applicationInfoBuildItem.getName(), FromApplication.FULL_PATTERN.pattern()))));
        }
    }

    @BuildStep
    void mapToQuarkusValidationErrorBuildItem(final List<ValidationErrorBuildItem> validationErrorBuildItems,
                                              final BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrorBuildItemProducer) {
        if (!validationErrorBuildItems.isEmpty()) {
            validationErrorBuildItemProducer.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    validationErrorBuildItems.stream().map(ValidationErrorBuildItem::getCause).toList()));
        }
    }

    @BuildStep
    RunTimeConfigurationDefaultBuildItem definePostgresCurrentSchema(final ApplicationInfoBuildItem applicationInfoBuildItem) {
        return new RunTimeConfigurationDefaultBuildItem("quarkus.datasource.jdbc.additional-jdbc-properties.currentSchema",
                applicationInfoBuildItem.getName().toLowerCase());
    }

    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> defaultConfigurations() {
        return List.of(
                new RunTimeConfigurationDefaultBuildItem("quarkus.datasource.jdbc.max-size", "100"));
    }

    @BuildStep
    ComposeServiceBuildItem generateCompose() {
        return PulseCommonProcessor.POSTGRES_COMPOSE_SERVICE_BUILD_ITEM;
    }

    public static ComposeServiceBuildItem.ServiceName POSTGRES_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME);

    public static ComposeServiceBuildItem.ServiceName KAFKA_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName("kafka");

    public static ComposeServiceBuildItem POSTGRES_COMPOSE_SERVICE_BUILD_ITEM = new ComposeServiceBuildItem(
            POSTGRES_SERVICE_NAME,
            new ComposeServiceBuildItem.ImageName("postgres:17.6-alpine3.22"),
            new ComposeServiceBuildItem.Labels(
                    Map.of("io.quarkus.devservices.compose.wait_for.logs", ".*database system is ready to accept connections.*")),
            new ComposeServiceBuildItem.Ports(List.of(PostgresUtils.DEFAULT_PORT + ":" + PostgresUtils.DEFAULT_PORT)),
            ComposeServiceBuildItem.Links.ofNone(),
            new ComposeServiceBuildItem.EnvironmentVariables(
                    Map.of("POSTGRES_USER", "quarkus",
                            "POSTGRES_DB", "quarkus",
                            "POSTGRES_PASSWORD", "quarkus")),
            new ComposeServiceBuildItem.Command(
                    List.of(
                            "postgres",
                            "-c", "wal_level=logical",
                            "-c", "hot_standby=on",
                            "-c", "max_wal_senders=10",
                            "-c", "max_replication_slots=10",
                            "-c", "synchronized_standby_slots=replication_slot")),
            ComposeServiceBuildItem.Entrypoint.ofNone(),
            new ComposeServiceBuildItem.HealthCheck(
                    List.of("CMD", "pg_isready"),
                    new ComposeServiceBuildItem.Interval(10),
                    new ComposeServiceBuildItem.Timeout(5),
                    new ComposeServiceBuildItem.Retries(5),
                    new ComposeServiceBuildItem.StartPeriod(10)),
            List.of(),
            ComposeServiceBuildItem.DependsOn.ofNone()
    );

    public static ComposeServiceBuildItem KAFKA_COMPOSE_SERVICE_BUILD_ITEM = new ComposeServiceBuildItem(
            KAFKA_SERVICE_NAME,
            new ComposeServiceBuildItem.ImageName("debezium-for-dev-service/kafka:4.2.0"),
            new ComposeServiceBuildItem.Labels(
                    Map.of("io.quarkus.devservices.compose.exposed_ports", "/tmp/ports")),
            new ComposeServiceBuildItem.Ports(List.of("9092", "29092")),
            ComposeServiceBuildItem.Links.ofNone(),
            new ComposeServiceBuildItem.EnvironmentVariables(
                    Map.of("CLUSTER_ID", "oh-sxaDRTcyAr6pFRbXyzA",
                            "NODE_ID", "1",
                            "KAFKA_CONTROLLER_QUORUM_VOTERS", "1@kafka:9093")),
            ComposeServiceBuildItem.Command.ofNone(),
            ComposeServiceBuildItem.Entrypoint.ofNone(),
            null,
            List.of(
// Not working for executable
// Even if `srcResolved.toFile().setExecutable(true);`
// Using KAFKA_LISTENERS=PLAINTEXT://172.24.0.3:9092,CONTROLLER://172.24.0.3:9093 and KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://172.24.0.3:9092
// /docker-entrypoint.sh: line 278: /kafka.sh: Permission denied
// Starting in KRaft mode, using CLUSTER_ID=oh-sxaDRTcyAr6pFRbXyzA, NODE_ID=1 and NODE_ROLE=combined.
// Using configuration config/server.properties.
// Using KAFKA_LISTENERS=PLAINTEXT://172.24.0.3:9092,CONTROLLER://172.24.0.3:9093 and KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://172.24.0.3:9092
// /docker-entrypoint.sh: line 278: /kafka.sh: Permission denied
//                    new ComposeServiceBuildItem.Volume("./kafka.sh", "/kafka.sh",
//                            // language=bash
//                            """
//                                    #!/bin/bash
//                                    set -euxo pipefail;
//                                    while [ ! -f /tmp/ports ]; do
//                                      sleep 0.1;
//                                    done;
//                                    sleep 0.1;
//                                    source /tmp/ports;
//                                    export KAFKA_LISTENERS=INTERNAL://0.0.0.0:29092,EXTERNAL://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093;
//                                    export KAFKA_ADVERTISED_LISTENERS=INTERNAL://kafka:29092,EXTERNAL://localhost:$PORT_9092;
//                                    export KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT;
//                                    export KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER;
//                                    export KAFKA_INTER_BROKER_LISTENER_NAME=INTERNAL;
//                                    exec /docker-entrypoint.sh start
//                                    """.getBytes(StandardCharsets.UTF_8))
            ),
            ComposeServiceBuildItem.DependsOn.on(List.of(POSTGRES_SERVICE_NAME)));

    /*
    2025-11-30 15:24:58,424 ERROR [io.qua.ver.htt.run.QuarkusErrorHandler] (executor-thread-1) HTTP Request to /prise_de_commande/1/ajouterPlat failed, error id: 3d488f63-b3a7-41e9-a9e5-b3d179b96e4e-1: com.damdamdeo.pulse.extension.core.encryption.DecryptionException: org.bouncycastle.openpgp.PGPException: cannot create cipher: No such provider: BC
        at com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPDecryptionService.decrypt(OpenPGPDecryptionService.java:68)
        at com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPDecryptionService_ClientProxy.decrypt(Unknown Source)
        at com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresEventRepository.loadOrderByVersionASC(JdbcPostgresEventRepository.java:134)
        at com.damdamdeo.pulse.extension.core.command.CommandHandler.lambda$handle$0(CommandHandler.java:30)
     */
    @BuildStep
    BouncyCastleProviderBuildItem bouncyCastleProviderBuildItemProducer() {
        return new BouncyCastleProviderBuildItem();
    }
}

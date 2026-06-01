package com.damdamdeo.pulse.extension.compose.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils.DEFAULT_PORT;
import static com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils.SERVICE_NAME;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class ComposeProcessor {

    private static final String DOCKER_COMPOSE_FILE = "../compose-devservices-pulse.yml";

    private static final String FEATURE = "pulse-compose-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    public static ComposeServiceBuildItem.ServiceName POSTGRES_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName(SERVICE_NAME);

    public static ComposeServiceBuildItem POSTGRES_COMPOSE_SERVICE_BUILD_ITEM = new ComposeServiceBuildItem(
            POSTGRES_SERVICE_NAME,
            new ComposeServiceBuildItem.ImageName("postgres:17.6-alpine3.22"),
            new ComposeServiceBuildItem.Labels(
                    Map.of("io.quarkus.devservices.compose.wait_for.logs", ".*database system is ready to accept connections.*")),
            new ComposeServiceBuildItem.Ports(List.of(DEFAULT_PORT + ":" + DEFAULT_PORT)),
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

    public static ComposeServiceBuildItem.ServiceName KAFKA_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName("kafka");

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

    public static class InlineList<T> extends ArrayList<T> {
        public InlineList(List<T> list) {
            super(list);
        }
    }

    @BuildStep
    void generateDockerCompose(final List<ComposeServiceBuildItem> composeServiceBuildItems,
                               final List<AdditionalVolumeBuildItem> additionalVolumeBuildItems,
                               final OutputTargetBuildItem outputTargetBuildItem,
                               // use the GeneratedResourceBuildItem only to ensure that the file will be created before compose is started
                               final BuildProducer<GeneratedResourceBuildItem> generatedResourceBuildItemBuildProducer) throws IOException {
        if (!composeServiceBuildItems.isEmpty()) {
            final List<ComposeServiceBuildItem.Volume> volumesToCreateOnHostSrc = new ArrayList<>();
            final Map<String, Object> root = new LinkedHashMap<>();
            final Map<String, Object> services = new LinkedHashMap<>();
            composeServiceBuildItems.forEach(composeServiceBuildItem -> {
                final Map<String, Object> service = new LinkedHashMap<>();
                final ComposeServiceBuildItem.ServiceName serviceName = composeServiceBuildItem.getServiceName();
                final ComposeServiceBuildItem.ImageName imageName = composeServiceBuildItem.getImageName();
                final ComposeServiceBuildItem.Labels labels = composeServiceBuildItem.getLabels();
                final ComposeServiceBuildItem.Ports ports = composeServiceBuildItem.getPorts();
                final ComposeServiceBuildItem.Links links = composeServiceBuildItem.getLinks();
                final ComposeServiceBuildItem.EnvironmentVariables environmentVariables = composeServiceBuildItem.getEnvironmentVariables();
                final ComposeServiceBuildItem.Command command = composeServiceBuildItem.getCommand();
                final ComposeServiceBuildItem.Entrypoint entrypoint = composeServiceBuildItem.getEntrypoint();
                final Optional<ComposeServiceBuildItem.HealthCheck> healthCheck = composeServiceBuildItem.getHealthCheck();
                final List<ComposeServiceBuildItem.Volume> volumes = composeServiceBuildItem.getVolumes();
                final ComposeServiceBuildItem.DependsOn dependsOn = composeServiceBuildItem.getDependsOn();
                service.put("image", imageName.name());
                if (labels.hasLabels()) {
                    service.put("labels", labels.labels());
                }
                if (serviceName.isInit()) {
                    service.put("restart", "no");
                } else {
                    service.put("restart", "always");
                }
                if (ports.hasPorts()) {
                    service.put("ports", ports.ports());
                }
                if (links.hasLinks()) {
                    service.put("links", links.links().stream().map(ComposeServiceBuildItem.ServiceName::name).toList());
                }
                if (environmentVariables.hasEnvironmentVariables()) {
                    service.put("environment", environmentVariables.environmentVariables().entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .toList());
                }
                if (command.hasCommand()) {
                    service.put("command", command.command());
                }
                if (entrypoint.hasEntrypoint()) {
                    service.put("entrypoint", new InlineList<>(entrypoint.entrypoint()));
                }
                healthCheck.ifPresent(value ->
                        service.put("healthcheck",
                                Map.of(
                                        "test", new InlineList<>(value.testCommand()),
                                        "interval", value.interval().inSeconds() + "s",
                                        "timeout", value.timeout().inSeconds() + "s",
                                        "retries", value.retries().numberOfRetries(),
                                        "start_period", value.startPeriod().inSeconds() + "s")));
                final List<ComposeServiceBuildItem.Volume> mergedVolumes = Stream.concat(
                                volumes.stream(),
                                additionalVolumeBuildItems.stream()
                                        .filter(additionalVolumeBuildItem -> serviceName.equals(additionalVolumeBuildItem.getServiceName()))
                                        .map(AdditionalVolumeBuildItem::getVolume))
                        .toList();
                if (!mergedVolumes.isEmpty()) {
                    service.put("volumes", mergedVolumes.stream()
                            .map(volume -> "%s:%s:ro,z".formatted(volume.src(), volume.destination()))
                            .toList());
                }
                volumesToCreateOnHostSrc.addAll(mergedVolumes);
                if (dependsOn.hasDependenciesOn()) {
                    service.put("depends_on", dependsOn.dependsOn().stream()
                            .map(ComposeServiceBuildItem.ServiceName::name)
                            .toList());
                }
                services.put(serviceName.name(), service);
            });
            root.put("services", services);

            final Path whereToCreate = outputTargetBuildItem.getOutputDirectory().getParent();
            for (final ComposeServiceBuildItem.Volume volume : volumesToCreateOnHostSrc) {
                final Path srcResolved = whereToCreate.resolve(volume.src().substring(2));
                Files.write(srcResolved, volume.content(), CREATE, TRUNCATE_EXISTING);
            }

            final DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setIndent(2);
            options.setPrettyFlow(true);

            final Representer representer = new Representer(options) {

                @Override
                protected org.yaml.snakeyaml.nodes.Node representSequence(final Tag tag,
                                                                          final Iterable<?> sequence,
                                                                          final DumperOptions.FlowStyle flowStyle) {
                    if (sequence instanceof InlineList<?>) {
                        // Force toutes les InlineList en FLOW
                        return super.representSequence(tag, sequence, DumperOptions.FlowStyle.FLOW);
                    }
                    return super.representSequence(tag, sequence, flowStyle);
                }
            };

            final Yaml yaml = new Yaml(representer, options);

            final Path resolved = outputTargetBuildItem.getOutputDirectory().resolve(DOCKER_COMPOSE_FILE);
            Files.createDirectories(resolved.getParent());
            try (final FileWriter writer = new FileWriter(resolved.toFile())) {
                yaml.dump(root, writer);
            }
        }
    }

}

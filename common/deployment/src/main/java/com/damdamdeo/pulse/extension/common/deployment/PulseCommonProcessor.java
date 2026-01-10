package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.PostgresSqlScriptBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.datasource.InitScriptUsageChecker;
import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseGenerator;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseProvider;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPDecryptionService;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.common.runtime.serialization.AllFieldsVisibilityObjectMapperCustomizer;
import com.damdamdeo.pulse.extension.common.runtime.vault.VaultPassphraseRepository;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.security.deployment.BouncyCastleProviderBuildItem;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class PulseCommonProcessor {

    private static final String DOCKER_COMPOSE_FILE = "../compose-devservices-pulse.yml";

    private static final String FEATURE = "pulse-common-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(AllFieldsVisibilityObjectMapperCustomizer.class)
                .addBeanClasses(VaultPassphraseRepository.class, DefaultPassphraseGenerator.class,
                        DefaultPassphraseProvider.class, OpenPGPDecryptionService.class,
                        OpenPGPEncryptionService.class,
                        InitScriptUsageChecker.class)
                .build();
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

    public static ComposeServiceBuildItem.ServiceName POSTGRES_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME);

    public static ComposeServiceBuildItem.ServiceName KAFKA_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName("kafka");

    public static ComposeServiceBuildItem POSTGRES_COMPOSE_SERVICE_BUILD_ITEM = new ComposeServiceBuildItem(
            POSTGRES_SERVICE_NAME,
            new ComposeServiceBuildItem.ImageName("postgres:17.6-alpine3.22"),
            new ComposeServiceBuildItem.Labels(
                    Map.of("io.quarkus.devservices.compose.wait_for.logs", ".*database system is ready to accept connections.*")),
            new ComposeServiceBuildItem.Ports(List.of(String.valueOf(PostgresUtils.DEFAULT_PORT))),
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
            new ComposeServiceBuildItem.ImageName("debezium-for-dev-service/kafka:3.3.1.Final"),
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
            new ComposeServiceBuildItem.HealthCheck(
                    List.of("CMD", "./bin/kafka-topics.sh", "--bootstrap-server", "kafka:29092", "--list"),
                    new ComposeServiceBuildItem.Interval(10),
                    new ComposeServiceBuildItem.Timeout(5),
                    new ComposeServiceBuildItem.Retries(5),
                    new ComposeServiceBuildItem.StartPeriod(10)),
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
    void generateSqlScripts(
            final Capabilities capabilities,
            final List<PostgresSqlScriptBuildItem> postgresSqlScriptBuildItems,
            final OutputTargetBuildItem outputTargetBuildItem,
            final BuildProducer<AdditionalVolumeBuildItem> additionalVolumeBuildItemProducer) throws IOException {
        if (capabilities.isPresent(Capability.FLYWAY)) {
            final Path scriptPulseInitialisationSql = outputTargetBuildItem.getOutputDirectory().resolve("classes/db/migration/V0__pulse_initialisation.sql");
            Files.createDirectories(scriptPulseInitialisationSql.getParent());
            final String content = postgresSqlScriptBuildItems.stream().map(PostgresSqlScriptBuildItem::getContent).collect(Collectors.joining("\r\n"));
            Files.writeString(scriptPulseInitialisationSql, content, CREATE, TRUNCATE_EXISTING);
        } else {
            postgresSqlScriptBuildItems.stream()
                    .map(sqlScriptBuildItem -> new AdditionalVolumeBuildItem(
                            PulseCommonProcessor.POSTGRES_SERVICE_NAME,
                            new ComposeServiceBuildItem.Volume(
                                    "./" + sqlScriptBuildItem.getName(),
                                    "/docker-entrypoint-initdb.d/" + sqlScriptBuildItem.getName(),
                                    sqlScriptBuildItem.getContent().getBytes(StandardCharsets.UTF_8)
                            )
                    ))
                    .forEach(additionalVolumeBuildItemProducer::produce);
        }
    }

    @BuildStep
    void defineFlywayConfiguration(final Capabilities capabilities,
                                   final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemProducer) {
        if (capabilities.isPresent(Capability.FLYWAY)) {
            runTimeConfigurationDefaultBuildItemProducer.produce(
                    new RunTimeConfigurationDefaultBuildItem("quarkus.flyway.migrate-at-start", "true"));
        }
    }

    private static final String ORG_FLYWAYDB_GROUP_ID = "org.flywaydb";
    private static final String FLYWAY_DATABASE_POSTGRESQL_ARTIFACT_ID = "flyway-database-postgresql";

    @BuildStep
    void validateFlywayDependency(final Capabilities capabilities,
                                  final CurateOutcomeBuildItem curateOutcomeBuildItem,
                                  final BuildProducer<ValidationErrorBuildItem> validationErrorBuildItemProducer) {
        if (capabilities.isPresent(Capability.FLYWAY) && curateOutcomeBuildItem.getApplicationModel().getDependencies().stream()
                .noneMatch(dep -> ORG_FLYWAYDB_GROUP_ID.equals(dep.getGroupId())
                        && FLYWAY_DATABASE_POSTGRESQL_ARTIFACT_ID.equals(dep.getArtifactId()))) {
            validationErrorBuildItemProducer.produce(new ValidationErrorBuildItem(
                    new IllegalStateException("Missing maven dependency %s:%s".formatted(ORG_FLYWAYDB_GROUP_ID, FLYWAY_DATABASE_POSTGRESQL_ARTIFACT_ID))));
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
                final ComposeServiceBuildItem.HealthCheck healthCheck = composeServiceBuildItem.getHealthCheck();
                final List<ComposeServiceBuildItem.Volume> volumes = composeServiceBuildItem.getVolumes();
                final ComposeServiceBuildItem.DependsOn dependsOn = composeServiceBuildItem.getDependsOn();
                service.put("image", imageName.name());
                if (labels.hasLabels()) {
                    service.put("labels", labels.labels());
                }
                service.put("restart", "always");
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
                service.put("healthcheck",
                        Map.of(
                                "test", new InlineList<>(healthCheck.testCommand()),
                                "interval", healthCheck.interval().inSeconds() + "s",
                                "timeout", healthCheck.timeout().inSeconds() + "s",
                                "retries", healthCheck.retries().numberOfRetries(),
                                "start_period", healthCheck.startPeriod().inSeconds() + "s"));
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

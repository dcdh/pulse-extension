package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.datasource.InitScriptUsageChecker;
import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresqlSchemaInitializer;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseGenerator;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseProvider;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPDecryptionService;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.common.runtime.serialization.AllFieldsVisibilityObjectMapperCustomizer;
import com.damdamdeo.pulse.extension.common.runtime.vault.VaultPassphraseRepository;
import com.damdamdeo.pulse.extension.core.consumer.ApplicationNaming;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                        InitScriptUsageChecker.class, PostgresqlSchemaInitializer.class)
                .build();
    }

    @BuildStep
    void validateApplicationNaming(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                   final BuildProducer<ValidationErrorBuildItem> validationErrorBuildItemProducer) {
        if (!ApplicationNaming.FULL_PATTERN.matcher(applicationInfoBuildItem.getName()).matches()) {
            validationErrorBuildItemProducer.produce(new ValidationErrorBuildItem(
                    new IllegalArgumentException(
                            "Invalid application name '%s' - it should match '%s'".formatted(applicationInfoBuildItem.getName(), ApplicationNaming.FULL_PATTERN.pattern()))));
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

    public static ComposeServiceBuildItem.ServiceName POSTGRES_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName("postgres");

    public static ComposeServiceBuildItem.ServiceName KAFKA_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName("kafka");

    public static ComposeServiceBuildItem POSTGRES_COMPOSE_SERVICE_BUILD_ITEM = new ComposeServiceBuildItem(
            POSTGRES_SERVICE_NAME,
            new ComposeServiceBuildItem.ImageName("postgres:17.6-alpine3.22"),
            new ComposeServiceBuildItem.Labels(
                    Map.of("io.quarkus.devservices.compose.wait_for.logs", ".*database system is ready to accept connections.*")),
            new ComposeServiceBuildItem.Ports(List.of("5432")),
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
            new ComposeServiceBuildItem.HealthCheck(
                    List.of("CMD", "pg_isready"),
                    new ComposeServiceBuildItem.Interval(10),
                    new ComposeServiceBuildItem.Timeout(5),
                    new ComposeServiceBuildItem.Retries(5),
                    new ComposeServiceBuildItem.StartPeriod(10)),
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
            new ComposeServiceBuildItem.HealthCheck(
                    List.of("CMD", "./bin/kafka-topics.sh", "--bootstrap-server", "kafka:29092", "--list"),
                    new ComposeServiceBuildItem.Interval(10),
                    new ComposeServiceBuildItem.Timeout(5),
                    new ComposeServiceBuildItem.Retries(5),
                    new ComposeServiceBuildItem.StartPeriod(10)),
            ComposeServiceBuildItem.DependsOn.on(List.of(POSTGRES_SERVICE_NAME)));

    public static class InlineList<T> extends ArrayList<T> {
        public InlineList(List<T> list) {
            super(list);
        }
    }

    @BuildStep
    void generateDockerCompose(final List<ComposeServiceBuildItem> composeServiceBuildItems,
                               final OutputTargetBuildItem outputTargetBuildItem,
                               // use the GeneratedResourceBuildItem only to ensure that the file will be created before compose is started
                               final BuildProducer<GeneratedResourceBuildItem> generatedResourceBuildItemBuildProducer) throws IOException {
        if (!composeServiceBuildItems.isEmpty()) {
            final Map<String, Object> root = new LinkedHashMap<>();
            final Map<String, Object> services = new LinkedHashMap<>();
            composeServiceBuildItems.forEach(composeServiceBuildItem -> {
                final Map<String, Object> service = new LinkedHashMap<>();
                final ComposeServiceBuildItem.ServiceName serviceName = composeServiceBuildItem.getServiceName();
                final ComposeServiceBuildItem.ImageName imageName = composeServiceBuildItem.getImageName();
                final ComposeServiceBuildItem.Labels labels = composeServiceBuildItem.getLabels();
                final ComposeServiceBuildItem.Ports ports = composeServiceBuildItem.getPorts();
                final ComposeServiceBuildItem.EnvironmentVariables environmentVariables = composeServiceBuildItem.getEnvironmentVariables();
                final ComposeServiceBuildItem.Command command = composeServiceBuildItem.getCommand();
                final ComposeServiceBuildItem.HealthCheck healthCheck = composeServiceBuildItem.getHealthCheck();
                final ComposeServiceBuildItem.DependsOn dependsOn = composeServiceBuildItem.getDependsOn();
                service.put("image", imageName.name());
                if (labels.hasLabels()) {
                    service.put("labels", labels.labels());
                }
                service.put("restart", "always");
                if (ports.hasPorts()) {
                    service.put("ports", ports.ports());
                }
                if (environmentVariables.hasEnvironmentVariables()) {
                    service.put("environment", environmentVariables.environmentVariables().entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .toList());
                }
                if (command.hasCommand()) {
                    service.put("command", command.command());
                }

                service.put("healthcheck",
                        Map.of(
                                "test", new InlineList<>(healthCheck.testCommand()),
                                "interval", healthCheck.interval().inSeconds() + "s",
                                "timeout", healthCheck.timeout().inSeconds() + "s",
                                "retries", healthCheck.retries().numberOfRetries(),
                                "start_period", healthCheck.startPeriod().inSeconds() + "s"));
                if (dependsOn.hasDependenciesOn()) {
                    service.put("depends_on", dependsOn.dependsOn().stream()
                            .map(ComposeServiceBuildItem.ServiceName::name)
                            .toList());
                }
                services.put(serviceName.name(), service);
            });
            root.put("services", services);

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

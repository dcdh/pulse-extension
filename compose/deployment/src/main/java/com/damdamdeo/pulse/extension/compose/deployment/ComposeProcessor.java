package com.damdamdeo.pulse.extension.compose.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.BasicTable;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.TableRow;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
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
import java.io.StringWriter;
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
            new ComposeServiceBuildItem.ImageName("postgres:18.4-alpine3.23"),
            new ComposeServiceBuildItem.Labels(
                    Map.of("io.quarkus.devservices.compose.wait_for.logs", ".*database system is ready to accept connections.*")),
            new ComposeServiceBuildItem.Ports(List.of(String.valueOf(DEFAULT_PORT))),
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

    public static class InlineList<T> extends ArrayList<T> {
        public InlineList(List<T> list) {
            super(list);
        }
    }

    @BuildStep
    void generateDockerCompose(final List<ComposeServiceBuildItem> composeServiceBuildItems,
                               final List<AdditionalVolumeBuildItem> additionalVolumeBuildItems,
                               final OutputTargetBuildItem outputTargetBuildItem,
                               final BuildProducer<ContentBuildItem> contentBuildItemProducer,
                               // use the GeneratedResourceBuildItem only to ensure that the file will be created before compose is started NOT THE CASE !!!
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
                final Map<String, String> allLabels = new HashMap<>(labels.labels());
                if (serviceName.isInit()) {
                    allLabels.put("io.quarkus.devservices.compose.ignore", "true");
                }
                if (labels.hasLabels()) {
                    service.put("labels", allLabels);
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
            final String dockerCompose;
            try (StringWriter stringWriter = new StringWriter()) {
                yaml.dump(root, stringWriter);
                dockerCompose = stringWriter.toString();
            }
            contentBuildItemProducer.produce(new ContentBuildItem(new Title(2, "Docker compose")));
            contentBuildItemProducer.produce(new ContentBuildItem(CodeBlock.fromYaml(dockerCompose)));
            contentBuildItemProducer.produce(new ContentBuildItem(new Title(2, "Additional volumes")));
            additionalVolumeBuildItems.forEach(additionalVolumeBuildItem -> {
                contentBuildItemProducer.produce(new ContentBuildItem(new Title(3, additionalVolumeBuildItem.getServiceName().name())));
                contentBuildItemProducer.produce(new ContentBuildItem(new BasicTable(
                        List.of(
                                new TableRow(List.of("src", additionalVolumeBuildItem.getVolume().src())),
                                new TableRow(List.of("destination", additionalVolumeBuildItem.getVolume().destination()))))));
                contentBuildItemProducer.produce(new ContentBuildItem(new CodeBlock(
                        additionalVolumeBuildItem.getVolume().contentType(),
                        new String(additionalVolumeBuildItem.getVolume().content())
                )));
            });
        }
    }

}

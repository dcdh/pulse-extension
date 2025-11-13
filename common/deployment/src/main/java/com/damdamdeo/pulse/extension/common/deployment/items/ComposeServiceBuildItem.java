package com.damdamdeo.pulse.extension.common.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ComposeServiceBuildItem extends MultiBuildItem {

    public record ServiceName(String name) {

        public ServiceName {
            Objects.requireNonNull(name);
            Validate.matchesPattern(name, "[a-z]+");
        }
    }

    public record ImageName(String name) {

        public ImageName {
            Objects.requireNonNull(name);
        }
    }


    public record Labels(Map<String, String> labels) {

        public Labels {
            Objects.requireNonNull(labels);
        }

        public static Labels ofNone() {
            return new Labels(Map.of());
        }

        public boolean hasLabels() {
            return !labels.isEmpty();
        }
    }

    public record Ports(List<String> ports) {

        public Ports {
            Objects.requireNonNull(ports);
        }

        public static Ports ofNone() {
            return new Ports(List.of());
        }

        public boolean hasPorts() {
            return !ports.isEmpty();
        }
    }

    public record Links(List<ServiceName> links) {

        public Links {
            Objects.requireNonNull(links);
        }

        public static Links ofNone() {
            return new Links(List.of());
        }

        public static Links on(List<ServiceName> links) {
            return new Links(links);
        }

        public boolean hasLinks() {
            return !links.isEmpty();
        }
    }

    public record EnvironmentVariables(Map<String, String> environmentVariables) {

        public EnvironmentVariables {
            Objects.requireNonNull(environmentVariables);
        }

        public static EnvironmentVariables ofNone() {
            return new EnvironmentVariables(Map.of());
        }

        public boolean hasEnvironmentVariables() {
            return !environmentVariables.isEmpty();
        }
    }

    public record Interval(Integer inSeconds) {

        public Interval {
            Objects.requireNonNull(inSeconds);
            Validate.validState(inSeconds > 0);
        }
    }

    public record Timeout(Integer inSeconds) {

        public Timeout {
            Objects.requireNonNull(inSeconds);
            Validate.validState(inSeconds > 0);
        }
    }

    public record Retries(Integer numberOfRetries) {

        public Retries {
            Objects.requireNonNull(numberOfRetries);
            Validate.validState(numberOfRetries > 0);
        }
    }

    public record StartPeriod(Integer inSeconds) {

        public StartPeriod {
            Objects.requireNonNull(inSeconds);
            Validate.validState(inSeconds > 0);
        }
    }

    public record HealthCheck(List<String> testCommand, Interval interval, Timeout timeout, Retries retries,
                              StartPeriod startPeriod) {

        public HealthCheck {
            Objects.requireNonNull(testCommand);
            Objects.requireNonNull(interval);
            Objects.requireNonNull(timeout);
            Objects.requireNonNull(retries);
            Objects.requireNonNull(startPeriod);
        }
    }

    public record Command(List<String> command) {

        public Command {
            Objects.requireNonNull(command);
        }

        public static Command ofNone() {
            return new Command(List.of());
        }

        public boolean hasCommand() {
            return !command.isEmpty();
        }
    }

    public record Entrypoint(List<String> entrypoint) {

        public Entrypoint {
            Objects.requireNonNull(entrypoint);
        }

        public static Entrypoint ofNone() {
            return new Entrypoint(List.of());
        }

        public boolean hasEntrypoint() {
            return !entrypoint.isEmpty();
        }
    }

    public record DependsOn(List<ServiceName> dependsOn) {

        public DependsOn {
            Objects.requireNonNull(dependsOn);
        }

        public static DependsOn ofNone() {
            return new DependsOn(List.of());
        }

        public static DependsOn on(final List<ServiceName> dependsOn) {
            return new DependsOn(dependsOn);
        }

        public boolean hasDependenciesOn() {
            return !dependsOn.isEmpty();
        }
    }

    // https://docs.docker.com/engine/storage/bind-mounts/#syntax
    public record Volume(String src, String destination, byte[] content) {

        public Volume {
            Objects.requireNonNull(src);
            Validate.validState(src.startsWith("./"));
            Objects.requireNonNull(destination);
            Objects.requireNonNull(content);
        }
    }

    private final ServiceName serviceName;
    private final ImageName imageName;
    private final Labels labels;
    private final Ports ports;
    private final Links links;
    private final EnvironmentVariables environmentVariables;
    private final Command command;
    private final Entrypoint entrypoint;
    private final HealthCheck healthCheck;
    private final List<Volume> volumes;
    private final DependsOn dependsOn;

    public ComposeServiceBuildItem(final ServiceName serviceName,
                                   final ImageName imageName,
                                   final Labels labels,
                                   final Ports ports,
                                   final Links links,
                                   final EnvironmentVariables environmentVariables,
                                   final Command command,
                                   final Entrypoint entrypoint,
                                   final HealthCheck healthCheck,
                                   final List<Volume> volumes,
                                   final DependsOn dependsOn) {
        this.serviceName = Objects.requireNonNull(serviceName);
        this.imageName = Objects.requireNonNull(imageName);
        this.labels = Objects.requireNonNull(labels);
        this.ports = Objects.requireNonNull(ports);
        this.links = Objects.requireNonNull(links);
        this.environmentVariables = Objects.requireNonNull(environmentVariables);
        this.command = Objects.requireNonNull(command);
        this.entrypoint = Objects.requireNonNull(entrypoint);
        this.healthCheck = Objects.requireNonNull(healthCheck);
        this.volumes = Objects.requireNonNull(volumes);
        this.dependsOn = Objects.requireNonNull(dependsOn);
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public ImageName getImageName() {
        return imageName;
    }

    public Labels getLabels() {
        return labels;
    }

    public Ports getPorts() {
        return ports;
    }

    public Links getLinks() {
        return links;
    }

    public EnvironmentVariables getEnvironmentVariables() {
        return environmentVariables;
    }

    public Command getCommand() {
        return command;
    }

    public Entrypoint getEntrypoint() {
        return entrypoint;
    }

    public HealthCheck getHealthCheck() {
        return healthCheck;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public DependsOn getDependsOn() {
        return dependsOn;
    }
}

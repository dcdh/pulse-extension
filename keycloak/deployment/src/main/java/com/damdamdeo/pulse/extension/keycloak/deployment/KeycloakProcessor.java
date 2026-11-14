package com.damdamdeo.pulse.extension.keycloak.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.KeycloakUtils;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfig;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.nio.file.Files;
import java.nio.file.Path;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

public class KeycloakProcessor {

    public static ComposeServiceBuildItem.ServiceName KEYCLOAK_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName(KeycloakUtils.SERVICE_NAME);

    public static BiFunction<KeycloakDevServicesConfig, Path, ComposeServiceBuildItem> KEYCLOAK_COMPOSE_SERVICE_BUILD_ITEM = (config, realmPath) -> {
        try {
            return new ComposeServiceBuildItem(
                    KEYCLOAK_SERVICE_NAME,
                    new ComposeServiceBuildItem.ImageName(config.imageName()
                            .orElse("keycloak/keycloak:26.6.2-2")),
                    new ComposeServiceBuildItem.Labels(
                            Map.of("io.quarkus.devservices.compose.wait_for.logs", ".*started in.*")),
                    new ComposeServiceBuildItem.Ports(List.of("8080")),
                    ComposeServiceBuildItem.Links.ofNone(),
                    new ComposeServiceBuildItem.EnvironmentVariables(
                            Map.of("KC_BOOTSTRAP_ADMIN_USERNAME", "admin",
                                    "KC_BOOTSTRAP_ADMIN_PASSWORD", "admin",
                                    "KC_PII_ENCKEY", "yourHighlySecure")),
                    new ComposeServiceBuildItem.Command(
                            List.of(
                                    "start-dev",
                                    "--features=declarative-ui",
                                    "--import-realm",
                                    "--spi-theme-static-max-age=-1",
                                    "--spi-theme-cache-themes=false",
                                    "--spi-theme-cache-templates=false")),
                    ComposeServiceBuildItem.Entrypoint.ofNone(),
                    null,
                    List.of(
                            new ComposeServiceBuildItem.FileVolume("./realm.json",
                                    "/opt/keycloak/data/import/realm.json",
                                    Files.exists(realmPath) ? Files.readAllBytes(realmPath) : KeycloakProcessor.class.getResourceAsStream("/realm.json").readAllBytes(), "json"),
                            new ComposeServiceBuildItem.DirectoryVolume("./keycloak-theme",
                                    "/opt/keycloak/themes/keycloak-theme")),
                    ComposeServiceBuildItem.DependsOn.ofNone());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    };

    @BuildStep
    void generateCompose(final BuildProducer<ComposeServiceBuildItem> composeServiceBuildItemBuildProducer,
                         final Capabilities capabilities,
                         final KeycloakDevServicesConfig keycloakDevServicesConfig,
                         final OutputTargetBuildItem outputTargetBuildItem) {
        if (capabilities.isPresent(Capability.OIDC)) {
            composeServiceBuildItemBuildProducer.produce(KEYCLOAK_COMPOSE_SERVICE_BUILD_ITEM.apply(keycloakDevServicesConfig,
                    outputTargetBuildItem.getOutputDirectory().getParent().resolve("realm.json")));
        }
    }
}

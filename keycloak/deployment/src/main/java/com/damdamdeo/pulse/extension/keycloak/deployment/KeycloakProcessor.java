package com.damdamdeo.pulse.extension.keycloak.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.KeycloakUtils;
import io.quarkus.deployment.annotations.BuildStep;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class KeycloakProcessor {

    public static ComposeServiceBuildItem.ServiceName KEYCLOAK_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName(KeycloakUtils.SERVICE_NAME);

    public static Supplier<ComposeServiceBuildItem> KEYCLOAK_COMPOSE_SERVICE_BUILD_ITEM = () -> {
        try {
            return new ComposeServiceBuildItem(
                    KEYCLOAK_SERVICE_NAME,
                    new ComposeServiceBuildItem.ImageName("keycloak/keycloak:26.6.2-2"),
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
                                    "--import-realm")),
                    ComposeServiceBuildItem.Entrypoint.ofNone(),
                    null,
                    List.of(new ComposeServiceBuildItem.Volume("./realm.json",
                            "/opt/keycloak/data/import/realm.json",
                            KeycloakProcessor.class.getResourceAsStream("/realm.json").readAllBytes(), "json"
                    )),
                    ComposeServiceBuildItem.DependsOn.ofNone());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    };

    @BuildStep
    ComposeServiceBuildItem generateCompose() {
        return KEYCLOAK_COMPOSE_SERVICE_BUILD_ITEM.get();
    }
}

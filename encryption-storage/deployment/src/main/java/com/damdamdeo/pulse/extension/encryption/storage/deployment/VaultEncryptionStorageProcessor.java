package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.Dependency;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class VaultEncryptionStorageProcessor {

    public static final Dependency QUARKUS_VAULT_DEPENDENCY = Dependency.of("io.quarkiverse.vault", "quarkus-vault");

    public static ComposeServiceBuildItem OPEN_BAO_COMPOSE_SERVICE_BUILD_ITEM = new ComposeServiceBuildItem(
            new ComposeServiceBuildItem.ServiceName("openbao"),
            new ComposeServiceBuildItem.ImageName("openbao/openbao:2.5.4"),
            new ComposeServiceBuildItem.Labels(
                    Map.of("io.quarkus.devservices.compose.wait_for.logs", ".*OpenBao server started!.*")),
            new ComposeServiceBuildItem.Ports(List.of("8200:8200")),
            ComposeServiceBuildItem.Links.ofNone(),
            new ComposeServiceBuildItem.EnvironmentVariables(
                    Map.of("BAO_DEV_ROOT_TOKEN_ID", "my-root-token",
                            "BAO_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")),
            new ComposeServiceBuildItem.Command(
                    List.of(
                            "server", "-dev", "-dev-no-store-token")),
            ComposeServiceBuildItem.Entrypoint.ofNone(),
            new ComposeServiceBuildItem.HealthCheck(
                    List.of("CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://127.0.0.0:8200"),
                    new ComposeServiceBuildItem.Interval(10),
                    new ComposeServiceBuildItem.Timeout(5),
                    new ComposeServiceBuildItem.Retries(3),
                    new ComposeServiceBuildItem.StartPeriod(5)),
            List.of(),
            ComposeServiceBuildItem.DependsOn.ofNone()
    );

    public static Function<ApplicationInfoBuildItem, ComposeServiceBuildItem> OPEN_BAO_INIT_COMPOSE_SERVICE_BUILD_ITEM = new Function<>() {

        @Override
        public ComposeServiceBuildItem apply(final ApplicationInfoBuildItem applicationInfoBuildItem) {
            final String applicationName = applicationInfoBuildItem.getName().toLowerCase();
            return new ComposeServiceBuildItem(
                    new ComposeServiceBuildItem.ServiceName("openbao-init"),
                    new ComposeServiceBuildItem.ImageName("openbao/openbao:2.5.4"),
                    new ComposeServiceBuildItem.Labels(
                            Map.of("io.quarkus.devservices.compose.wait_for.logs", ".*Success! Uploaded policy.*")),
                    new ComposeServiceBuildItem.Ports(List.of()),
                    ComposeServiceBuildItem.Links.on(List.of(new ComposeServiceBuildItem.ServiceName("openbao"))),
                    new ComposeServiceBuildItem.EnvironmentVariables(
                            Map.of("BAO_ADDR", "http://openbao:8200",
                                    "BAO_TOKEN", "my-root-token")),
                    ComposeServiceBuildItem.Command.ofNone(),
                    new ComposeServiceBuildItem.Entrypoint(List.of(
                            // TODO bind user and password using quarkus.vault.authentication.userpass.username and quarkus.vault.authentication.userpass.password
                            // Warning may not possible because username and password are runtime values
                            // cf https://docs.quarkiverse.io/quarkus-vault/dev/index.html#_starting_vault regarding userpass, bob and sinclair
                            "sh", "-c", "sleep 10;bao policy write %1$s_pulse-app-policy /tmp/%1$s_pulse-policy-policy.hcl;bao auth enable userpass;bao write auth/userpass/users/bob password=sinclair policies=%1$s_pulse-app-policy".formatted(applicationName)
                    )),
                    null,
                    List.of(new ComposeServiceBuildItem.Volume("./%1$s_pulse-policy-policy.hcl".formatted(applicationName),
                            "/tmp/%1$s_pulse-policy-policy.hcl".formatted(applicationName),
                            // language=hcl
                            """
                                    path "secret/data/%1$s/owner/*" {
                                        capabilities = ["create", "read", "delete", "list", "update"]
                                    }
                                    path "secret/metadata/" {
                                        capabilities = ["list"]
                                    }
                                    path "secret/metadata/%1$s" {
                                        capabilities = ["list"]
                                    }
                                    path "secret/metadata/%1$s/owner/*" {
                                        capabilities = ["list", "read", "delete", "update"]
                                    }
                                    """
                                    .formatted(applicationName)
                                    .getBytes(StandardCharsets.UTF_8)
                    )),
                    ComposeServiceBuildItem.DependsOn.on(List.of(new ComposeServiceBuildItem.ServiceName("openbao")))
            );
        }
    };

    @BuildStep
    List<ComposeServiceBuildItem> generateCompose(final CurateOutcomeBuildItem curateOutcomeBuildItem,
                                                  final ApplicationInfoBuildItem applicationInfoBuildItem) {
        if (EncryptionStorageProcessor.hasDependency(curateOutcomeBuildItem, QUARKUS_VAULT_DEPENDENCY)) {
            return List.of(OPEN_BAO_COMPOSE_SERVICE_BUILD_ITEM, OPEN_BAO_INIT_COMPOSE_SERVICE_BUILD_ITEM.apply(applicationInfoBuildItem));
        } else {
            return List.of();
        }
    }
}

package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.damdamdeo.pulse.extension.encryption.storage.deployment.EncryptionStorageProcessor.hasVaultInClassPath;

public class VaultEncryptionStorageProcessor {

    public static ComposeServiceBuildItem OPEN_BAO_COMPOSE_SERVICE_BUILD_ITEM = new ComposeServiceBuildItem(
            new ComposeServiceBuildItem.ServiceName("openbao"),
            new ComposeServiceBuildItem.ImageName("openbao/openbao:2.5.4"),
            new ComposeServiceBuildItem.Labels(
                    Map.of("io.quarkus.devservices.compose.wait_for.logs", ".*OpenBao server started!.*")),
            new ComposeServiceBuildItem.Ports(List.of("8200")),
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

    public static ComposeServiceBuildItem OPEN_BAO_INIT_COMPOSE_SERVICE_BUILD_ITEM = new ComposeServiceBuildItem(
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
                    "sh", "-c", "sleep 10;bao policy write pulse-app-policy /tmp/pulse-policy-policy.hcl;bao auth enable userpass;bao write auth/userpass/users/bob password=sinclair policies=pulse-app-policy"
            )),
            null,
            List.of(new ComposeServiceBuildItem.Volume("./pulse-policy-policy.hcl", "/tmp/pulse-policy-policy.hcl",
                    // language=hcl
                    """
                            path "secret/data/owner/*" {
                                capabilities = ["create", "read", "delete", "list", "update"]
                            }
                            path "secret/metadata/" {
                                capabilities = ["list"]
                            }
                            path "secret/metadata/owner/*" {
                                capabilities = ["list", "read", "delete", "update"]
                            }
                            """.getBytes(StandardCharsets.UTF_8), "hcl"
            )),
            ComposeServiceBuildItem.DependsOn.on(List.of(new ComposeServiceBuildItem.ServiceName("openbao")))
    );

    @BuildStep
    List<ComposeServiceBuildItem> generateCompose() {
        if (hasVaultInClassPath.get()) {
            return List.of(OPEN_BAO_COMPOSE_SERVICE_BUILD_ITEM, OPEN_BAO_INIT_COMPOSE_SERVICE_BUILD_ITEM);
        } else {
            return List.of();
        }
    }
}

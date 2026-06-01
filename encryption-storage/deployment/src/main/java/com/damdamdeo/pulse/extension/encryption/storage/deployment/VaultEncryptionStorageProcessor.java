package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.vault.VaultPassphraseRepository;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.Dependency;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VaultEncryptionStorageProcessor {

    // no capability exposed by vault yet ...
    // https://github.com/quarkiverse/quarkus-vault/pull/507
    // check on dependency presence instead, which is bad
    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans(final Capabilities capabilities,
                                                  final CurateOutcomeBuildItem curateOutcomeBuildItem) {
        final List<AdditionalBeanBuildItem> additionalBeanBuildItems = new ArrayList<>();
//        if (capabilities.isPresent("io.quarkiverse.vault")) {
//            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
//                    .addBeanClasses(VaultPassphraseRepository.class)
//                    .build());
//        }
        if (hasDependency(curateOutcomeBuildItem, Dependency.of("io.quarkiverse.vault", "quarkus-vault"))) {
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(VaultPassphraseRepository.class)
                    .build());
        }
        return additionalBeanBuildItems;
    }
    /*
version: '3.8'

services:
  openbao:
    image: openbao/openbao:2.5.4
    container_name: openbao-dev
    ports:
      - "8200:8200"
    environment:
      BAO_DEV_ROOT_TOKEN_ID: "my-root-token"
      BAO_DEV_LISTEN_ADDRESS: "0.0.0.0:8200"
    command: ["server", "-dev", "-dev-no-store-token"]


  openbao-init:
    image: openbao/openbao:2.5.4
    container_name: openbao-init
    environment:
      BAO_ADDR: "http://openbao:8200"
      BAO_TOKEN: "my-root-token"
    volumes:
      - ./quarkus-policy.hcl:/tmp/quarkus-policy.hcl
    depends_on:
      - openbao
    entrypoint: >
      sh -c "
        sleep 2;
        bao policy write quarkus-app /tmp/quarkus-policy.hcl;
        bao token create -policy=quarkus-app -id='quarkus-app-token';
        bao kv put secret/myapps/config database-password='super-secret-password';
        echo 'Permissions et secrets initialisés dans OpenBao !';
      "

// quarkus-policy.hcl
# Autorise la lecture des secrets sous OpenBao
path "secret/data/myapps/config" {
  capabilities = ["read"]
}

path "secret/myapps/config" {
  capabilities = ["read"]
}
     */

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
                            """.getBytes(StandardCharsets.UTF_8)
            )),
            ComposeServiceBuildItem.DependsOn.on(List.of(new ComposeServiceBuildItem.ServiceName("openbao")))
    );

    @BuildStep
    List<ComposeServiceBuildItem> generateCompose() {
        return List.of(OPEN_BAO_COMPOSE_SERVICE_BUILD_ITEM, OPEN_BAO_INIT_COMPOSE_SERVICE_BUILD_ITEM);
    }

    private boolean hasDependency(final CurateOutcomeBuildItem curateOutcomeBuildItem,
                                  final Dependency dependency) {
        return curateOutcomeBuildItem.getApplicationModel()
                .getDependencies()
                .stream()
                .anyMatch(dep ->
                        dep.getGroupId().equals(dependency.getGroupId())
                                && dep.getArtifactId().equals(dependency.getArtifactId()));
    }
}

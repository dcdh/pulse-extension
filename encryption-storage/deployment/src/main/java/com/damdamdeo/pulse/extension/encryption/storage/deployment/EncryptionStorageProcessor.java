package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.encryption.storage.runtime.vault.JdbcPostgresPassphraseRepository;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.vault.VaultPassphraseRepository;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.Dependency;

import java.util.ArrayList;
import java.util.List;

public class EncryptionStorageProcessor {

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
        } else if (hasDependency(curateOutcomeBuildItem, Dependency.of("io.quarkus", "quarkus-jdbc-postgresql"))) {
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(JdbcPostgresPassphraseRepository.class)
                    .build());
        }
        return additionalBeanBuildItems;
    }

    public static boolean hasDependency(final CurateOutcomeBuildItem curateOutcomeBuildItem,
                                         final Dependency dependency) {
        return curateOutcomeBuildItem.getApplicationModel()
                .getDependencies()
                .stream()
                .anyMatch(dep ->
                        dep.getGroupId().equals(dependency.getGroupId())
                                && dep.getArtifactId().equals(dependency.getArtifactId()));
    }
}

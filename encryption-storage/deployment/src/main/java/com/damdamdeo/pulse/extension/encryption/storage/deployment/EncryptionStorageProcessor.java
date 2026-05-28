package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.encryption.storage.runtime.vault.VaultPassphraseRepository;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

import java.util.ArrayList;
import java.util.List;

public class EncryptionStorageProcessor {

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        final List<AdditionalBeanBuildItem> additionalBeanBuildItems = new ArrayList<>();
        additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                .addBeanClasses(VaultPassphraseRepository.class)
                .build());
        return additionalBeanBuildItems;
    }
}

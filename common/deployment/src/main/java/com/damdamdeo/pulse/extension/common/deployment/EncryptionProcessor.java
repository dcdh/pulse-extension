package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.runtime.encryption.CachedPassphraseProvider;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseGenerator;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPDecryptionService;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.core.encryption.DefaultPassphraseProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

import java.util.List;

public class EncryptionProcessor {

    @BuildStep
    AdditionalBeanBuildItem produceAdditionalBeanBuildItem() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(
                        DefaultPassphraseGenerator.class,
                        DefaultPassphraseProvider.class,
                        OpenPGPDecryptionService.class,
                        OpenPGPEncryptionService.class)
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .setUnremovable()
                .build();
    }

    @BuildStep
    List<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return List.of(new AdditionalIndexedClassesBuildItem(CachedPassphraseProvider.class.getName()));
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeanBuildItems(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return List.of(AdditionalBeanBuildItem.builder().addBeanClass(CachedPassphraseProvider.class).build());
        } else {
            return List.of();
        }
    }

}

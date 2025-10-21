package com.damdamdeo.pulse.extension.deployment;

import com.damdamdeo.pulse.extension.core.command.JvmCommandHandlerRegistry;
import com.damdamdeo.pulse.extension.runtime.*;
import com.damdamdeo.pulse.extension.runtime.encryption.DefaultPassphraseGenerator;
import com.damdamdeo.pulse.extension.runtime.encryption.DefaultPassphraseProvider;
import com.damdamdeo.pulse.extension.runtime.encryption.OpenPGPDecryptionService;
import com.damdamdeo.pulse.extension.runtime.encryption.VaultPassphraseRepository;
import com.damdamdeo.pulse.extension.runtime.serialization.AllFieldsVisibilityObjectMapperCustomizer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import java.util.List;

class PulseExtensionProcessor {

    private static final String FEATURE = "pulse-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        return List.of(
                AdditionalBeanBuildItem.builder()
                        .addBeanClasses(DefaultQuarkusTransaction.class, DefaultInstantProvider.class)
                        .addBeanClass(PostgresqlEventStoreInitializer.class)
                        .addBeanClass(AllFieldsVisibilityObjectMapperCustomizer.class)
                        .addBeanClasses(VaultPassphraseRepository.class, DefaultPassphraseGenerator.class,
                                DefaultPassphraseProvider.class, OpenPGPDecryptionService.class)
                        .build(),
                // TODO it is not possible to define the bean with @DefaultBean
                // Should conditionally add it if no other implementation is present.
                AdditionalBeanBuildItem.builder()
                        .addBeanClass(JvmCommandHandlerRegistry.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .build()
        );
    }
}

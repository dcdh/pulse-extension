package com.damdamdeo.pulse.extension.deployment;

import com.damdamdeo.pulse.extension.core.command.JvmCommandHandlerRegistry;
import com.damdamdeo.pulse.extension.runtime.DefaultInstantProvider;
import com.damdamdeo.pulse.extension.runtime.DefaultQuarkusTransaction;
import com.damdamdeo.pulse.extension.runtime.PostgresqlEventStoreInitializer;
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
                        .addBeanClass(PostgresqlEventStoreInitializer.class).build(),
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

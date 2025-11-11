package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.PulseCommonProcessor;
import com.damdamdeo.pulse.extension.common.deployment.items.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresqlSchemaInitializer;
import com.damdamdeo.pulse.extension.core.command.JvmCommandHandlerRegistry;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultInstantProvider;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultQuarkusTransaction;
import com.damdamdeo.pulse.extension.writer.runtime.PostgresqlEventStoreInitializer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import java.util.List;

public class PulseWriterProcessor {

    private static final String FEATURE = "pulse-writer-extension";

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
                        .addBeanClass(PostgresqlSchemaInitializer.class)
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

    @BuildStep
    ComposeServiceBuildItem generateCompose() {
        return PulseCommonProcessor.POSTGRES_COMPOSE_SERVICE_BUILD_ITEM;
    }
}

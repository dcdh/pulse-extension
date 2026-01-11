package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.PostgresSqlScriptBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.datasource.DatabaseInitializationService;
import com.damdamdeo.pulse.extension.common.runtime.datasource.InitializationScriptsProvider;
import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresSqlScript;
import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresSqlScriptsRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import jakarta.inject.Singleton;

import java.util.List;

public class InitializationProcessor {

    @BuildStep
    void additionalBeanBuildItemProduce(final Capabilities capabilities,
                                        final BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            additionalBeanBuildItemBuildProducer.produce(
                    AdditionalBeanBuildItem.builder().addBeanClass(DatabaseInitializationService.class).build());
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void initializeInitializationScriptsProvider(final Capabilities capabilities,
                                                 final PostgresSqlScriptsRecorder postgresSqlScriptsRecorder,
                                                 final List<PostgresSqlScriptBuildItem> postgresSqlScriptBuildItems,
                                                 final BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            final List<PostgresSqlScript> postgresSqlScripts = postgresSqlScriptBuildItems.stream()
                    .map(postgresSqlScriptBuildItem -> new PostgresSqlScript(postgresSqlScriptBuildItem.getName(), postgresSqlScriptBuildItem.getContent()))
                    .toList();
            syntheticBeanBuildItemBuildProducer.produce(
                    SyntheticBeanBuildItem.configure(InitializationScriptsProvider.class)
                            .scope(Singleton.class)
                            .runtimeValue(postgresSqlScriptsRecorder.provide(postgresSqlScripts))
                            .done());
        }
    }
}

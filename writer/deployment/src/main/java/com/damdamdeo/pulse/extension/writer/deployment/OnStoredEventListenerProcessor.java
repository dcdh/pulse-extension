package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.listener.AbstractOnStoredEventListener;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

import java.util.List;

public class OnStoredEventListenerProcessor {

    @BuildStep
    List<AdditionalBeanBuildItem> registerOnStoredEventListeners(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex().getAllKnownSubclasses(AbstractOnStoredEventListener.class)
                .stream()
                .map(onStoredEventListener -> AdditionalBeanBuildItem.builder()
                        .addBeanClass(onStoredEventListener.name().toString())
                        .setDefaultScope(DotNames.SINGLETON)
                        .setUnremovable()
                        .build())
                .toList();
    }
}

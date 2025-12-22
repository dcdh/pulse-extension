package com.damdamdeo.pulse.extension.livenotifier.deployment.consumer.notifier;

import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier.SseBroadcasterEndpoint;
import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier.UnknownClientProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

public class LiveNotifierProcessor {

    @BuildStep
    AdditionalBeanBuildItem produceAdditionalBeanBuildItem() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(SseBroadcasterEndpoint.class)
                .addBeanClass(UnknownClientProvider.class)
                .setUnremovable().build();
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem produceAdditionalIndexedClassesBuildItem() {
        return new AdditionalIndexedClassesBuildItem(SseBroadcasterEndpoint.class.getName());
    }
}

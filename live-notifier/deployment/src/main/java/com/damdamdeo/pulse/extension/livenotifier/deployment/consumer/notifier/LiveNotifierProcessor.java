package com.damdamdeo.pulse.extension.livenotifier.deployment.consumer.notifier;

import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier.QuarkusOidcClientProvider;
import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier.SseBroadcasterEndpoint;
import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier.UnknownClientProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

public class LiveNotifierProcessor {

    @BuildStep
    AdditionalBeanBuildItem produceAdditionalBeanBuildItem(final Capabilities capabilities) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (capabilities.isPresent("io.quarkus.oidc")) {
            builder.addBeanClass(QuarkusOidcClientProvider.class);
        }
        return builder
                .addBeanClass(SseBroadcasterEndpoint.class)
                .addBeanClass(UnknownClientProvider.class)
                .setUnremovable().build();
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem produceAdditionalIndexedClassesBuildItem() {
        return new AdditionalIndexedClassesBuildItem(SseBroadcasterEndpoint.class.getName());
    }
}

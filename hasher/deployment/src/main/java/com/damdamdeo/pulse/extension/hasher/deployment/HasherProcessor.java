package com.damdamdeo.pulse.extension.hasher.deployment;

import com.damdamdeo.pulse.extension.hasher.runtime.Sha3256DefaultHasher;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import java.util.ArrayList;
import java.util.List;

public class HasherProcessor {

    private static final String FEATURE = "pulse-hasher-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        final List<AdditionalBeanBuildItem> additionalBeanBuildItems = new ArrayList<>();
        additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                .addBeanClasses(Sha3256DefaultHasher.class)
                .build());
        return additionalBeanBuildItems;
    }
}

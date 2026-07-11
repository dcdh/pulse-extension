package com.damdamdeo.pulse.extension.query.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class PulseQueryProcessor {

    private static final String FEATURE = "pulse-query-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}

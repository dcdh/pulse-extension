package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseProvider;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

public class CacheProcessor {

    @BuildStep
    AdditionalIndexedClassesBuildItem produceIndexedClassesBuildItem() {
        return new AdditionalIndexedClassesBuildItem(DefaultPassphraseProvider.class.getName());
    }
}

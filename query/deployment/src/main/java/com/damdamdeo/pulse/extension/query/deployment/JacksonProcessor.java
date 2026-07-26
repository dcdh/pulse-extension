package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.query.runtime.gdpr.EncryptionModuleCustomizer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class JacksonProcessor {

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder().addBeanClass(EncryptionModuleCustomizer.class).build();
    }
}

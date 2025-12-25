package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.runtime.executedby.QuarkusOidcExecutedByProvider;
import com.damdamdeo.pulse.extension.core.executedby.NotAvailableExecutedByProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;

public class ExecutedByProcessor {

    @BuildStep
    AdditionalBeanBuildItem produceAdditionalBeanBuildItem(final Capabilities capabilities) {
        if (capabilities.isPresent("io.quarkus.oidc")) {
            return AdditionalBeanBuildItem.builder().addBeanClass(QuarkusOidcExecutedByProvider.class).build();
        } else {
            return AdditionalBeanBuildItem.builder()
                    .addBeanClass(NotAvailableExecutedByProvider.class)
                    .setDefaultScope(DotNames.APPLICATION_SCOPED)
                    .setUnremovable()
                    .build();
        }
    }
}

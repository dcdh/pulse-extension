package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.runtime.executedby.DefaultOwnedByExecutedByDecoder;
import com.damdamdeo.pulse.extension.common.runtime.executedby.DefaultOwnedByExecutedByEncoder;
import com.damdamdeo.pulse.extension.common.runtime.executedby.QuarkusOidcExecutedByProvider;
import com.damdamdeo.pulse.extension.core.executedby.NotAvailableExecutedByProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;

import java.util.ArrayList;
import java.util.List;

public class ExecutedByProcessor {

    @BuildStep
    List<AdditionalBeanBuildItem> produceAdditionalBeanBuildItem(final Capabilities capabilities) {
        final List<AdditionalBeanBuildItem> additionalBeanBuildItems = new ArrayList<>();
        additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder().addBeanClasses(DefaultOwnedByExecutedByDecoder.class).build());
        additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder().addBeanClasses(DefaultOwnedByExecutedByEncoder.class).build());
        if (capabilities.isPresent("io.quarkus.oidc")) {
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder().addBeanClasses(QuarkusOidcExecutedByProvider.class).build());
        } else {
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClass(NotAvailableExecutedByProvider.class)
                    .setDefaultScope(DotNames.APPLICATION_SCOPED)
                    .setUnremovable()
                    .build());
        }
        return additionalBeanBuildItems;
    }
}

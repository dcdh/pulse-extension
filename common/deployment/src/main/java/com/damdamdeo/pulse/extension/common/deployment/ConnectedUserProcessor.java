package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.runtime.connecteduser.ConnectedUserNotAvailableUserProvider;
import com.damdamdeo.pulse.extension.common.runtime.connecteduser.QuarkusOidcConnectedUserProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;

import java.util.ArrayList;
import java.util.List;

public class ConnectedUserProcessor {

    @BuildStep
    List<AdditionalBeanBuildItem> produceAdditionalBeanBuildItem(final Capabilities capabilities) {
        final List<AdditionalBeanBuildItem> additionalBeanBuildItems = new ArrayList<>();
        if (capabilities.isPresent(Capability.OIDC)) {
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(QuarkusOidcConnectedUserProvider.class)
                    .build());
        } else {
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(ConnectedUserNotAvailableUserProvider.class)
                    .build());
        }
        return additionalBeanBuildItems;
    }
}

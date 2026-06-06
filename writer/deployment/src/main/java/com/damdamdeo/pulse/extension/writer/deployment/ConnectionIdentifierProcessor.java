package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierAssociation;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;

public class ConnectionIdentifierProcessor {

    @BuildStep
    AdditionalBeanBuildItem additionalBeans(final Capabilities capabilities) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (capabilities.isPresent(Capability.OIDC)) {
            builder.addBeanClasses(ConnectionIdentifierAssociation.class)
                    .setDefaultScope(DotNames.APPLICATION_SCOPED)
                    .setUnremovable();
        }
        return builder.build();
    }
}

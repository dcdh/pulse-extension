package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.runtime.audience.JdbcPostgresExecutedByResolver;
import com.damdamdeo.pulse.extension.common.runtime.audience.SmallryeConfigBackendUserVisibilityRolesProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class BeansProcessor {

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(SmallryeConfigBackendUserVisibilityRolesProvider.class,
                        JdbcPostgresExecutedByResolver.class)
                .build();
    }
}

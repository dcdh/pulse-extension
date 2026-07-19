package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.query.runtime.JdbcPostgresExecutedByResolver;
import com.damdamdeo.pulse.extension.query.runtime.QueryExceptionMapper;
import com.damdamdeo.pulse.extension.query.runtime.SmallryeConfigBackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.JdbcPostgresOwnedByProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

public class BeansProcessor {

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(SmallryeConfigBackendUserVisibilityRolesProvider.class,
                        JdbcPostgresExecutedByResolver.class, JdbcPostgresOwnedByProvider.class)
                .build();
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem additionalIndexedClasses() {
        return new AdditionalIndexedClassesBuildItem(QueryExceptionMapper.class.getName());
    }
}

package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.query.runtime.JdbcPostgresExecutedByResolver;
import com.damdamdeo.pulse.extension.query.runtime.QueryExceptionMapper;
import com.damdamdeo.pulse.extension.query.runtime.SmallryeConfigBackendUserVisibilityRolesProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class PulseQueryProcessor {

    private static final String FEATURE = "pulse-query-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(SmallryeConfigBackendUserVisibilityRolesProvider.class,
                        JdbcPostgresExecutedByResolver.class)
                .build();
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem additionalIndexedClasses() {
        return new AdditionalIndexedClassesBuildItem(QueryExceptionMapper.class.getName());
    }
}

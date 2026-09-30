package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.runtime.audience.JdbcPostgresExecutedByResolver;
import com.damdamdeo.pulse.extension.common.runtime.audience.SmallryeConfigBackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.common.runtime.featureflag.ArcFeaturesProvider;
import com.damdamdeo.pulse.extension.common.runtime.featureflag.FeatureEndpoint;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

public class BeansProcessor {

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(SmallryeConfigBackendUserVisibilityRolesProvider.class,
                        JdbcPostgresExecutedByResolver.class,
                        ArcFeaturesProvider.class)
                .build();
    }

    @BuildStep
    void registerFeatureEndpoint(final BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItemBuildProducer,
                                 final BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalIndexedClassesBuildItemBuildProducer.produce(new AdditionalIndexedClassesBuildItem(FeatureEndpoint.class.getName()));
        additionalBeanBuildItemBuildProducer.produce(AdditionalBeanBuildItem.builder().addBeanClasses(FeatureEndpoint.class).build());
    }
}

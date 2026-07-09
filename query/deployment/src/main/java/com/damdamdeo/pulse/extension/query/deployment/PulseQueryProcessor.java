package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.query.Query;
import com.damdamdeo.pulse.extension.query.runtime.CdiGuardQueryDecorator;
import com.damdamdeo.pulse.extension.query.runtime.QueryExceptionMapper;
import com.damdamdeo.pulse.extension.query.runtime.SmallryeConfigBackendUserVisibilityRolesProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import java.util.List;
import java.util.stream.Stream;

public class PulseQueryProcessor {

    private static final String FEATURE = "pulse-query-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(SmallryeConfigBackendUserVisibilityRolesProvider.class)
                .build();
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem additionalIndexedClasses() {
        return new AdditionalIndexedClassesBuildItem(QueryExceptionMapper.class.getName(),
                CdiGuardQueryDecorator.class.getName());
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerQuery(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return Stream.concat(
                        combinedIndexBuildItem.getIndex().getAllKnownImplementations(Query.class)
                                .stream()
                                .map(query -> AdditionalBeanBuildItem.builder()
                                        .addBeanClass(query.name().toString())
                                        .setDefaultScope(DotNames.SINGLETON)
                                        .setUnremovable()
                                        .build()),
                        Stream.of(AdditionalBeanBuildItem.builder()
                                .addBeanClasses(CdiGuardQueryDecorator.class)
                                .build()))
                .toList();
    }
}

package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.usecase.UseCase;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

import java.util.List;

public class UseCaseProcessor {

    @BuildStep
    List<AdditionalBeanBuildItem> registerUseCase(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex().getAllKnownImplementations(UseCase.class)
                .stream()
                .map(useCase -> AdditionalBeanBuildItem.builder()
                        .addBeanClass(useCase.name().toString())
                        .setDefaultScope(DotNames.SINGLETON)
                        .setUnremovable()
                        .build())
                .toList();
    }
}

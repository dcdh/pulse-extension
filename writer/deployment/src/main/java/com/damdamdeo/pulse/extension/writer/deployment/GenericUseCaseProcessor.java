package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.usecase.GenericUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCase;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import jakarta.transaction.Transactional;
import org.jboss.jandex.*;

import java.util.List;

public class GenericUseCaseProcessor {

    @BuildStep
    List<AdditionalBeanBuildItem> registerGenericUseCase(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex().getAllKnownImplementations(GenericUseCase.class)
                .stream()
                .map(useCase -> AdditionalBeanBuildItem.builder()
                        .addBeanClass(useCase.name().toString())
                        .setDefaultScope(DotNames.SINGLETON)
                        .setUnremovable()
                        .build())
                .toList();
    }

    @BuildStep
    AnnotationsTransformerBuildItem addTransactionalToUseCases(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final IndexView index = combinedIndexBuildItem.getIndex();
        final AnnotationValue valueBusinessException = AnnotationValue.createClassValue("", Type.create(
                DotName.createSimple(BusinessException.class),
                Type.Kind.CLASS
        ));
        // RuntimeException likes TechnicalException are always rolled back.
        // Not needed to add it to rollbackOn
        final AnnotationValue populatedRollbackOn = AnnotationValue.createArrayValue(
                "rollbackOn",
                new AnnotationValue[]{valueBusinessException});
        final AnnotationInstance transactionalAnnotation = AnnotationInstance.create(
                DotName.createSimple(Transactional.class),
                null, // target
                new AnnotationValue[]{populatedRollbackOn});
        return new AnnotationsTransformerBuildItem(
                (AnnotationTransformation) context -> {
                    final Declaration target = context.declaration();
                    if (target.kind() == AnnotationTarget.Kind.CLASS) {
                        if (UtilsProcessor.hasDirectImplementation(target.asClass(), index, GenericUseCase.class)
                                || UtilsProcessor.hasDirectImplementation(target.asClass(), index, UseCase.class)) {
                            context.add(transactionalAnnotation);
                        }
                    }
                }
        );
    }
}

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
                        if (implementsUseCase(target.asClass(), index)) {
                            context.add(transactionalAnnotation);
                        }
                    }
                }
        );
    }

    private boolean implementsUseCase(final ClassInfo classInfo, final IndexView index) {
        if (classInfo.interfaceNames().contains(DotName.createSimple(UseCase.class))
                || classInfo.interfaceNames().contains(DotName.createSimple(GenericUseCase.class))) {
            return true;
        }
        for (final DotName interfaceName : classInfo.interfaceNames()) {
            final ClassInfo interfaceInfo = index.getClassByName(interfaceName);
            if (interfaceInfo != null && implementsUseCase(interfaceInfo, index)) {
                return true;
            }
        }
        return false;
    }
}

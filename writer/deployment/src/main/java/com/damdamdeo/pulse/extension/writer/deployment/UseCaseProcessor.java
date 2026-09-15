package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.connecteduser.registration.AbstractRegistrationDomainUseCase;
import com.damdamdeo.pulse.extension.core.connecteduser.update.AbstractUpdateUserNameUseCase;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
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
    AdditionalBeanBuildItem registerAggregateIdDecomposer() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(AggregateIdDecomposer.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build();
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerDomainUseCase(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex().getAllKnownImplementations(DomainUseCase.class)
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
        final AnnotationValue valueUseCaseException = AnnotationValue.createClassValue("", Type.create(
                DotName.createSimple(UseCaseException.class),
                Type.Kind.CLASS
        ));
        // RuntimeException likes TechnicalException are always rolled back.
        // Not needed to add it to rollbackOn
        final AnnotationValue populatedRollbackOn = AnnotationValue.createArrayValue(
                "rollbackOn",
                new AnnotationValue[]{valueUseCaseException});
        final AnnotationInstance transactionalAnnotation = AnnotationInstance.create(
                DotName.createSimple(Transactional.class),
                null, // target
                new AnnotationValue[]{populatedRollbackOn});
        return new AnnotationsTransformerBuildItem(
                (AnnotationTransformation) context -> {
                    final Declaration target = context.declaration();
                    if (target.kind() == AnnotationTarget.Kind.CLASS) {
                        if (UtilsProcessor.hasDirectImplementation(target.asClass(), index, DomainUseCase.class)
                                || UtilsProcessor.hasSuperClass(target.asClass(), index, AbstractRegistrationDomainUseCase.class)
                                || UtilsProcessor.hasSuperClass(target.asClass(), index, AbstractUpdateUserNameUseCase.class)) {
                            context.add(transactionalAnnotation);
                        }
                    }
                }
        );
    }
}

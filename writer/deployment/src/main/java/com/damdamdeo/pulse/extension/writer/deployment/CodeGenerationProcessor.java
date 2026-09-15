package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;
import com.damdamdeo.pulse.extension.core.query.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.query.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.GuardDomainUseCase;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;

import java.lang.reflect.Modifier;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;

public class CodeGenerationProcessor {

    @BuildStep
    void generateGuardDomainUseCase(final CombinedIndexBuildItem combinedIndexBuildItem,
                                    final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                    final OutputTargetBuildItem outputTargetBuildItem) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(DomainUseCase.class)
                .forEach(domainUseCaseClassInfo -> {
                    try {
                        final org.jboss.jandex.Type domainUseCaseInterface = domainUseCaseClassInfo.interfaceTypes().stream()
                                .filter(t -> t.name().equals(DotName.createSimple(DomainUseCase.class)))
                                .findFirst()
                                .orElseThrow();

                        final ParameterizedType parameterizedType = domainUseCaseInterface.asParameterizedType();
                        final Class<?> aggregateIdClass = classLoader.loadClass(
                                parameterizedType.arguments().getFirst().name().toString());

                        final Class<?> commandClass = classLoader.loadClass(
                                parameterizedType.arguments().get(1).name().toString());

                        final Class<?> aggregateRootClass = classLoader.loadClass(
                                parameterizedType.arguments().get(2).name().toString());

                        final Class<?> queryClass = classLoader.loadClass(domainUseCaseClassInfo.name().toString());
                        try (final ClassCreator beanClassCreator = ClassCreator.builder()
                                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                                .className(queryClass.getName().replaceAll("\\$", "_") + "GuardDomainUseCaseGenerated")
                                .signature(SignatureBuilder.forClass()
                                        .setSuperClass(
                                                Type.parameterizedType(
                                                        Type.classType(GuardDomainUseCase.class),
                                                        Type.classType(aggregateIdClass),
                                                        Type.classType(commandClass),
                                                        Type.classType(aggregateRootClass))))
                                .setFinal(true)
                                .build()) {
                            beanClassCreator.addAnnotation(Unremovable.class);
                            beanClassCreator.addAnnotation(Decorator.class);

                            beanClassCreator.addAnnotation(AnnotationInstance.create(
                                    DotName.createSimple(Priority.class),
                                    null,
                                    new AnnotationValue[]{AnnotationValue.createIntegerValue("value", 1)}
                            ));

                            try (final MethodCreator constructor = beanClassCreator.getMethodCreator("<init>", void.class,
                                    ExecutionContextProvider.class, BackendUserVisibilityRolesProvider.class, ExecutedByResolver.class,
                                    AggregateIdDecomposer.class, DomainUseCase.class)) {
                                constructor
                                        .setSignature(SignatureBuilder.forMethod()
                                                .addParameterType(Type.classType(ExecutionContextProvider.class))
                                                .addParameterType(Type.classType(BackendUserVisibilityRolesProvider.class))
                                                .addParameterType(Type.classType(ExecutedByResolver.class))
                                                .addParameterType(Type.classType(AggregateIdDecomposer.class))
                                                .addParameterType(Type.parameterizedType(
                                                        Type.classType(DomainUseCase.class),
                                                        Type.classType(aggregateIdClass),
                                                        Type.classType(commandClass),
                                                        Type.classType(aggregateRootClass)))
                                                .build());
                                constructor.getParameterAnnotations(4).addAnnotation(Any.class);
                                constructor.getParameterAnnotations(4).addAnnotation(Delegate.class);
                                constructor.setModifiers(Modifier.PUBLIC);
                                constructor.invokeSpecialMethod(
                                        MethodDescriptor.ofConstructor(GuardDomainUseCase.class,
                                                ExecutionContextProvider.class, BackendUserVisibilityRolesProvider.class,
                                                ExecutedByResolver.class, AggregateIdDecomposer.class, DomainUseCase.class),
                                        constructor.getThis(),
                                        constructor.getMethodParam(0),
                                        constructor.getMethodParam(1),
                                        constructor.getMethodParam(2),
                                        constructor.getMethodParam(3),
                                        constructor.getMethodParam(4)
                                );

                                constructor.returnValue(null);
                            }

                            writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}

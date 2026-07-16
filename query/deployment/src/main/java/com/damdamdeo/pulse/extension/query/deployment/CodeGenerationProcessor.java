package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.query.runtime.JdbcProjectionFromEventStore;
import io.quarkus.arc.DefaultBean;
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
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;

import java.lang.reflect.Modifier;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;

public class CodeGenerationProcessor {

    @BuildStep
    void generateJdbcProjectionFromEventStore(final CombinedIndexBuildItem combinedIndexBuildItem,
                                              final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                              final OutputTargetBuildItem outputTargetBuildItem) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Projection.class)
                .forEach(projectionClassInfo -> {
                    try {
                        final Class<?> projectionClass = classLoader.loadClass(projectionClassInfo.name().toString());
                        try (final ClassCreator beanClassCreator = ClassCreator.builder()
                                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                                .className(projectionClass.getName().replaceAll("\\$", "_") + "JdbcProjectionFromEventStoreGenerated")
                                .signature(SignatureBuilder.forClass()
                                        .setSuperClass(
                                                Type.parameterizedType(
                                                        Type.classType(JdbcProjectionFromEventStore.class),
                                                        Type.classType(projectionClass))))
                                .setFinal(false) // must be false when using @Transactional
                                .build()) {
                            beanClassCreator.addAnnotation(Transactional.class);
                            beanClassCreator.addAnnotation(Singleton.class);
                            beanClassCreator.addAnnotation(Unremovable.class);
                            beanClassCreator.addAnnotation(DefaultBean.class);

                            try (final MethodCreator getAggregateClass = beanClassCreator.getMethodCreator("getProjectionClass", Class.class)) {
                                getAggregateClass.setModifiers(Modifier.PROTECTED);
                                getAggregateClass.returnValue(getAggregateClass.loadClass(projectionClass));
                            }

                            writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @BuildStep
    void generateGuardQuery(final CombinedIndexBuildItem combinedIndexBuildItem,
                            final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                            final OutputTargetBuildItem outputTargetBuildItem) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Query.class)
                .forEach(queryClassInfo -> {
                    try {
                        final org.jboss.jandex.Type queryInterface = queryClassInfo.interfaceTypes().stream()
                                .filter(t -> t.name().equals(DotName.createSimple(Query.class)))
                                .findFirst()
                                .orElseThrow();

                        final ParameterizedType parameterizedType = queryInterface.asParameterizedType();
                        final Class<?> inputClass = classLoader.loadClass(
                                parameterizedType.arguments().getFirst().name().toString());

                        Class<?> projectionClass = classLoader.loadClass(
                                parameterizedType.arguments().get(1).name().toString());

                        final Class<?> queryClass = classLoader.loadClass(queryClassInfo.name().toString());
                        try (final ClassCreator beanClassCreator = ClassCreator.builder()
                                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                                .className(queryClass.getName().replaceAll("\\$", "_") + "GuardQueryGenerated")
                                .signature(SignatureBuilder.forClass()
                                        .setSuperClass(
                                                Type.parameterizedType(
                                                        Type.classType(GuardQuery.class),
                                                        Type.classType(inputClass),
                                                        Type.classType(projectionClass))))
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
                                    ExecutionContextProvider.class, BackendUserVisibilityRolesProvider.class, ExecutedByResolver.class, Query.class)) {
                                constructor
                                        .setSignature(SignatureBuilder.forMethod()
                                                .addParameterType(Type.classType(ExecutionContextProvider.class))
                                                .addParameterType(Type.classType(BackendUserVisibilityRolesProvider.class))
                                                .addParameterType(Type.classType(ExecutedByResolver.class))
                                                .addParameterType(Type.parameterizedType(
                                                        Type.classType(Query.class),
                                                        Type.classType(inputClass),
                                                        Type.classType(projectionClass)))
                                                .build());
                                constructor.getParameterAnnotations(3).addAnnotation(Any.class);
                                constructor.getParameterAnnotations(3).addAnnotation(Delegate.class);
                                constructor.setModifiers(Modifier.PUBLIC);
                                constructor.invokeSpecialMethod(
                                        MethodDescriptor.ofConstructor(GuardQuery.class, ExecutionContextProvider.class, BackendUserVisibilityRolesProvider.class, ExecutedByResolver.class, Query.class),
                                        constructor.getThis(),
                                        constructor.getMethodParam(0),
                                        constructor.getMethodParam(1),
                                        constructor.getMethodParam(2),
                                        constructor.getMethodParam(3)
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

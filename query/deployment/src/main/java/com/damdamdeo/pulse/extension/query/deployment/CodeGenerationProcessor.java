package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.query.runtime.JdbcProjectionFromEventStore;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Type;
import jakarta.inject.Singleton;

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
                                .className(projectionClass.getName().replaceAll("\\$", "_") + "JdbcProjectionFromEventStore")
                                .signature(SignatureBuilder.forClass()
                                        .setSuperClass(
                                                Type.parameterizedType(
                                                        Type.classType(JdbcProjectionFromEventStore.class),
                                                        Type.classType(projectionClass))))
                                .setFinal(true)
                                .build()) {
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
}

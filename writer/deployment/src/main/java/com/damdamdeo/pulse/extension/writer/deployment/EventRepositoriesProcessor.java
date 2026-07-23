package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.writer.deployment.items.AggregateRootBuildItem;
import com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresEventRepository;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Type;
import jakarta.inject.Singleton;

import java.lang.reflect.Modifier;
import java.util.List;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;

public class EventRepositoriesProcessor {

    @BuildStep
    void generateEventRepositories(final List<AggregateRootBuildItem> aggregateRootBuildItems,
                                   final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                   final OutputTargetBuildItem outputTargetBuildItem) {
        aggregateRootBuildItems.forEach(aggregateRootBuildItem -> {
            try (final ClassCreator beanClassCreator = ClassCreator.builder()
                    .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                    .className(aggregateRootBuildItem.aggregateRootClazz().getName().replaceAll("\\$", "_") + "JdbcEventRepositoryGenerated")
                    .signature(SignatureBuilder.forClass()
                            .setSuperClass(
                                    Type.parameterizedType(
                                            Type.classType(JdbcPostgresEventRepository.class),
                                            Type.classType(aggregateRootBuildItem.aggregateRootClazz()),
                                            Type.classType(aggregateRootBuildItem.aggregateIdClazz()))))
                    .setFinal(true)
                    .build()) {
                beanClassCreator.addAnnotation(Singleton.class);
                beanClassCreator.addAnnotation(Unremovable.class);
                beanClassCreator.addAnnotation(DefaultBean.class);

                try (final MethodCreator getAggregateClass = beanClassCreator.getMethodCreator("getAggregateClass", Class.class)) {
                    getAggregateClass.setModifiers(Modifier.PROTECTED);
                    getAggregateClass.returnValue(
                            getAggregateClass.loadClass(aggregateRootBuildItem.aggregateRootClazz()));
                }

                writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
            }
        });
    }
}

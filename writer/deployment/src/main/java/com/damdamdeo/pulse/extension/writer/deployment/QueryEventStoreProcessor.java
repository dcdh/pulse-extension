package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.event.InternalQueryEventStore;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.writer.deployment.items.AggregateRootBuildItem;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;
import jakarta.inject.Singleton;

import java.lang.reflect.Modifier;
import java.util.List;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;

public class QueryEventStoreProcessor {

    @BuildStep
    void generateQueryEventStore(final List<AggregateRootBuildItem> aggregateRootBuildItems,
                                 final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                 final OutputTargetBuildItem outputTargetBuildItem) {
        aggregateRootBuildItems.forEach(aggregateRootBuildItem -> {
            try (final ClassCreator beanClassCreator = ClassCreator.builder()
                    .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                    .className(aggregateRootBuildItem.aggregateRootClazz().getName().replaceAll("\\$", "_") + "QueryEventStoreGenerated")
                    .signature(SignatureBuilder.forClass()
                            .setSuperClass(
                                    Type.parameterizedType(
                                            Type.classType(InternalQueryEventStore.class),
                                            Type.classType(aggregateRootBuildItem.aggregateRootClazz()),
                                            Type.classType(aggregateRootBuildItem.aggregateIdClazz()))))
                    .setFinal(true)
                    .build()) {
                beanClassCreator.addAnnotation(Singleton.class);
                beanClassCreator.addAnnotation(Unremovable.class);
                beanClassCreator.addAnnotation(DefaultBean.class);

                try (final MethodCreator constructor = beanClassCreator.getMethodCreator("<init>", void.class,
                        EventRepository.class, ExecutionContextProvider.class)) {
                    constructor
                            .setSignature(SignatureBuilder.forMethod()
                                    .addParameterType(Type.parameterizedType(
                                            Type.classType(EventRepository.class),
                                            Type.classType(aggregateRootBuildItem.aggregateRootClazz()),
                                            Type.classType(aggregateRootBuildItem.aggregateIdClazz())))
                                    .addParameterType(Type.classType(ExecutionContextProvider.class))
                                    .build());
                    constructor.setModifiers(Modifier.PUBLIC);

                    constructor.invokeSpecialMethod(
                            MethodDescriptor.ofConstructor(InternalQueryEventStore.class, EventRepository.class, ExecutionContextProvider.class),
                            constructor.getThis(),
                            constructor.getMethodParam(0),
                            constructor.getMethodParam(1)
                    );

                    constructor.returnValue(null);
                }

                try (final MethodCreator getAggregateRootClass = beanClassCreator.getMethodCreator("getAggregateRootClass", Class.class)) {
                    getAggregateRootClass.setModifiers(Modifier.PROTECTED);
                    getAggregateRootClass.returnValue(
                            getAggregateRootClass.loadClass(aggregateRootBuildItem.aggregateRootClazz()));
                }

                try (final MethodCreator getAggregateRootClass = beanClassCreator.getMethodCreator("getAggregateIdClass", Class.class)) {
                    getAggregateRootClass.setModifiers(Modifier.PROTECTED);
                    getAggregateRootClass.returnValue(
                            getAggregateRootClass.loadClass(aggregateRootBuildItem.aggregateIdClazz()));
                }

                writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
            }
        });
    }
}

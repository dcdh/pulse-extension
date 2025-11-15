package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CommandHandlerRegistry;
import com.damdamdeo.pulse.extension.core.command.Transaction;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.event.QueryEventStore;
import com.damdamdeo.pulse.extension.core.projection.Projection;
import com.damdamdeo.pulse.extension.writer.deployment.items.AggregateRootBuildItem;
import com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresEventRepository;
import com.damdamdeo.pulse.extension.writer.runtime.projection.JdbcProjectionFromEventStore;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class CodeGenerationProcessor {

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
                                            Type.classType(QueryEventStore.class),
                                            Type.classType(aggregateRootBuildItem.aggregateRootClazz()),
                                            Type.classType(aggregateRootBuildItem.aggregateIdClazz()))))
                    .setFinal(true)
                    .build()) {
                beanClassCreator.addAnnotation(Singleton.class);
                beanClassCreator.addAnnotation(Unremovable.class);
                beanClassCreator.addAnnotation(DefaultBean.class);

                try (final MethodCreator constructor = beanClassCreator.getMethodCreator("<init>", void.class,
                        EventRepository.class)) {
                    constructor
                            .setSignature(SignatureBuilder.forMethod()
                                    .addParameterType(Type.parameterizedType(
                                            Type.classType(EventRepository.class),
                                            Type.classType(aggregateRootBuildItem.aggregateRootClazz()),
                                            Type.classType(aggregateRootBuildItem.aggregateIdClazz())))
                                    .build());
                    constructor.setModifiers(Modifier.PUBLIC);

                    constructor.invokeSpecialMethod(
                            MethodDescriptor.ofConstructor(QueryEventStore.class, EventRepository.class),
                            constructor.getThis(),
                            constructor.getMethodParam(0)
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

    @BuildStep
    void generateCommandHandlers(final List<AggregateRootBuildItem> aggregateRootBuildItems,
                                 final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                 final OutputTargetBuildItem outputTargetBuildItem) {
        aggregateRootBuildItems.forEach(aggregateRootBuildItem -> {
            try (final ClassCreator beanClassCreator = ClassCreator.builder()
                    .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                    .className(aggregateRootBuildItem.aggregateRootClazz().getName().replaceAll("\\$", "_") + "CommandHandlerGenerated")
                    .signature(SignatureBuilder.forClass()
                            .setSuperClass(
                                    Type.parameterizedType(
                                            Type.classType(CommandHandler.class),
                                            Type.classType(aggregateRootBuildItem.aggregateRootClazz()),
                                            Type.classType(aggregateRootBuildItem.aggregateIdClazz()))))
                    .setFinal(true)
                    .build()) {
                beanClassCreator.addAnnotation(Singleton.class);
                beanClassCreator.addAnnotation(Unremovable.class);
                beanClassCreator.addAnnotation(DefaultBean.class);

                try (final MethodCreator constructor = beanClassCreator.getMethodCreator("<init>", void.class,
                        CommandHandlerRegistry.class, EventRepository.class, Transaction.class)) {
                    constructor
                            .setSignature(SignatureBuilder.forMethod()
                                    .addParameterType(Type.classType(CommandHandlerRegistry.class))
                                    .addParameterType(Type.parameterizedType(
                                            Type.classType(EventRepository.class),
                                            Type.classType(aggregateRootBuildItem.aggregateRootClazz()),
                                            Type.classType(aggregateRootBuildItem.aggregateIdClazz())))
                                    .addParameterType(Type.classType(Transaction.class)).build());
                    constructor.setModifiers(Modifier.PUBLIC);

                    constructor.invokeSpecialMethod(
                            MethodDescriptor.ofConstructor(CommandHandler.class,
                                    CommandHandlerRegistry.class,
                                    EventRepository.class,
                                    Transaction.class),
                            constructor.getThis(),
                            constructor.getMethodParam(0),
                            constructor.getMethodParam(1),
                            constructor.getMethodParam(2)
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

    public static void writeGeneratedClass(final ClassCreator classCreator, final OutputTargetBuildItem outputTargetBuildItem) {
        classCreator.writeTo((name, data) -> {
            final Path classGeneratedPath = outputTargetBuildItem.getOutputDirectory().resolve(name.substring(name.lastIndexOf("/") + 1) + ".class");
            try {
                if (Files.notExists(classGeneratedPath)) {
                    Files.createFile(classGeneratedPath);
                }
                Files.write(classGeneratedPath, data, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

package com.damdamdeo.pulse.extension.deployment;

import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CommandHandlerRegistry;
import com.damdamdeo.pulse.extension.core.command.Transaction;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.event.QueryEventStore;
import com.damdamdeo.pulse.extension.deployment.items.AggregateRootBuildItem;
import com.damdamdeo.pulse.extension.runtime.JdbcEventRepository;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
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
                                            Type.classType(JdbcEventRepository.class),
                                            Type.classType(aggregateRootBuildItem.aggregateRootClazz()),
                                            Type.classType(aggregateRootBuildItem.aggregateIdClazz()))))
                    .setFinal(true)
                    .build()) {
                beanClassCreator.addAnnotation(Singleton.class);
                beanClassCreator.addAnnotation(DefaultBean.class);

                try (final MethodCreator getAggregateClass = beanClassCreator.getMethodCreator("getAggregateClass", Class.class)) {
                    getAggregateClass.setModifiers(Modifier.PROTECTED);
                    getAggregateClass.returnValue(
                            getAggregateClass.loadClass(aggregateRootBuildItem.aggregateRootClazz()));
                }

                beanClassCreator.writeTo((name, data) -> {
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

                try (final MethodCreator getAggregateClass = beanClassCreator.getMethodCreator("getAggregateClass", Class.class)) {
                    getAggregateClass.setModifiers(Modifier.PROTECTED);
                    getAggregateClass.returnValue(
                            getAggregateClass.loadClass(aggregateRootBuildItem.aggregateRootClazz()));
                }

                beanClassCreator.writeTo((name, data) -> {
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

                try (final MethodCreator getAggregateClass = beanClassCreator.getMethodCreator("getAggregateClass", Class.class)) {
                    getAggregateClass.setModifiers(Modifier.PROTECTED);
                    getAggregateClass.returnValue(
                            getAggregateClass.loadClass(aggregateRootBuildItem.aggregateRootClazz()));
                }

                beanClassCreator.writeTo((name, data) -> {
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
        });
    }

}

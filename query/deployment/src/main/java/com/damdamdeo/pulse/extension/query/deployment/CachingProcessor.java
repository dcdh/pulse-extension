package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.ProjectionFromEventStore;
import com.damdamdeo.pulse.extension.query.runtime.CachedProjectionFromEventStore;
import com.damdamdeo.pulse.extension.query.runtime.EventCounter;
import com.damdamdeo.pulse.extension.query.runtime.JdbcEventCounter;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.CachedOwnedByProvider;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.deployment.spi.AdditionalCacheNameBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;

public class CachingProcessor {

    private static final String CACHE_NAME = "projection";

    private static final Map<String, String> CACHING_CONFIGURATIONS = Map.of(
            "quarkus.cache.caffeine.\"projection\".initial-capacity", "10000",
            "quarkus.cache.caffeine.\"projection\".maximum-size", "10000",
            "quarkus.cache.caffeine.\"projection\".expire-after-write", "7D",
            "quarkus.cache.caffeine.\"ownedBy\".initial-capacity", "10000",
            "quarkus.cache.caffeine.\"ownedBy\".maximum-size", "10000",
            "quarkus.cache.caffeine.\"ownedBy\".expire-after-write", "7D");

    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> defineDefaultConfiguration(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return CACHING_CONFIGURATIONS.entrySet().stream()
                    .map(e -> new RunTimeConfigurationDefaultBuildItem(e.getKey(), e.getValue()))
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<ContentBuildItem> contentBuildItems(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            final List<ContentBuildItem> contentBuildItems = new ArrayList<>();
            contentBuildItems.add(new ContentBuildItem(new Title(Title.Level.SECOND, "Projection caching configuration")));
            contentBuildItems.add(new ContentBuildItem(CodeBlock.fromProperties(CACHING_CONFIGURATIONS)));
            return contentBuildItems;
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return List.of(new AdditionalIndexedClassesBuildItem(CachedOwnedByProvider.class.getName()));
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeanBuildItems(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return List.of(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(CachedOwnedByProvider.class, JdbcEventCounter.class)
                    .build());
        } else {
            return List.of();
        }
    }

    @BuildStep
    void generateCachedProjectionFromEventStore(final Capabilities capabilities,
                                                final CombinedIndexBuildItem combinedIndexBuildItem,
                                                final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                                final OutputTargetBuildItem outputTargetBuildItem) {
        if (capabilities.isPresent(Capability.CACHE)) {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            combinedIndexBuildItem.getIndex()
                    .getAllKnownImplementations(Projection.class)
                    .forEach(projectionClassInfo -> {
                        try {
                            final Class<?> projectionClass = classLoader.loadClass(projectionClassInfo.name().toString());
                            final String className = projectionClass.getName().replaceAll("\\$", "_") + "CachedProjectionFromEventStore";
                            try (final ClassCreator beanClassCreator = ClassCreator.builder()
                                    .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                                    .className(className)
                                    .signature(SignatureBuilder.forClass()
                                            .setSuperClass(
                                                    Type.parameterizedType(
                                                            Type.classType(CachedProjectionFromEventStore.class),
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
                                        ProjectionFromEventStore.class, EventCounter.class, Cache.class)) {
                                    constructor
                                            .setSignature(SignatureBuilder.forMethod()
                                                    .addParameterType(Type.parameterizedType(
                                                            Type.classType(ProjectionFromEventStore.class),
                                                            Type.classType(projectionClass)))
                                                    .addParameterType(Type.classType(EventCounter.class))
                                                    .addParameterType(Type.classType(Cache.class))
                                                    .build());
                                    constructor.getParameterAnnotations(0).addAnnotation(Any.class);
                                    constructor.getParameterAnnotations(0).addAnnotation(Delegate.class);

                                    constructor.getParameterAnnotations(2).addAnnotation(Inject.class);
                                    constructor.getParameterAnnotations(2).addAnnotation(
                                            AnnotationInstance.create(
                                                    DotName.createSimple(CacheName.class),
                                                    null,
                                                    new AnnotationValue[]{AnnotationValue.createStringValue("value", CACHE_NAME)}
                                            ));

                                    constructor.setModifiers(Modifier.PUBLIC);
                                    constructor.invokeSpecialMethod(
                                            MethodDescriptor.ofConstructor(CachedProjectionFromEventStore.class,
                                                    ProjectionFromEventStore.class, EventCounter.class, Cache.class),
                                            constructor.getThis(),
                                            constructor.getMethodParam(0),
                                            constructor.getMethodParam(1),
                                            constructor.getMethodParam(2)
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

    @BuildStep
    List<AdditionalCacheNameBuildItem> registerProjectionCache(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return List.of(new AdditionalCacheNameBuildItem(CACHE_NAME));
        } else {
            return List.of();
        }
    }
}

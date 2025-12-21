package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.writer.runtime.EventClazzDiscovery;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;
import jakarta.inject.Singleton;
import org.jboss.jandex.ClassInfo;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;
import static io.quarkus.gizmo.Type.parameterizedType;

public class EventClazzDiscoveryProcessor {

    @BuildStep
    void generateMixinRegistrarObjectMapper(final CombinedIndexBuildItem combinedIndexBuildItem,
                                            final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                            final OutputTargetBuildItem outputTargetBuildItem) {
        final List<ClassInfo> events = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Event.class)
                .stream()
                .toList();

        try (final ClassCreator beanClassCreator = ClassCreator.builder()
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                .className(EventClazzDiscovery.class.getName() + "Generated")
                .superClass(EventClazzDiscovery.class)
                .build()) {

            beanClassCreator.addAnnotation(Singleton.class);
            beanClassCreator.addAnnotation(Unremovable.class);

            final MethodCreator mappingsMethod = beanClassCreator.getMethodCreator("mappings", Map.class);
            mappingsMethod.setSignature(
                    SignatureBuilder.forMethod()
                            .setReturnType(parameterizedType(
                                    io.quarkus.gizmo.Type.classType(Map.class),
                                    io.quarkus.gizmo.Type.classType(String.class),
                                    io.quarkus.gizmo.Type.classType(String.class)))
                            .build());
            mappingsMethod.setModifiers(Modifier.PROTECTED);

            final ResultHandle map = mappingsMethod.newInstance(MethodDescriptor.ofConstructor(HashMap.class));

            events.forEach(event -> {
                final ResultHandle eventSimpleName = mappingsMethod.load(event.simpleName());
                final ResultHandle eventName = mappingsMethod.load(event.name().toString());
                mappingsMethod.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(HashMap.class, "put", Object.class, Object.class, Object.class),
                        map, eventSimpleName, eventName);
            });
            mappingsMethod.returnValue(map);
            writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
        }
    }
}

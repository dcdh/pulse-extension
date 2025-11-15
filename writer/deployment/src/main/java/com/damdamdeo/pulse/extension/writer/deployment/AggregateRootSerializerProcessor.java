package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.writer.deployment.items.DiscoveredClassBuildItem;
import com.damdamdeo.pulse.extension.writer.runtime.MixinRegistrationObjectMapperCustomizer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.Validate;
import org.jboss.jandex.*;
import org.jboss.jandex.Type;

import java.lang.reflect.Modifier;
import java.util.*;

import static io.quarkus.gizmo.Type.parameterizedType;

public class AggregateRootSerializerProcessor {

    @BuildStep
    public List<DiscoveredClassBuildItem> discoverAggregateFields(final CombinedIndexBuildItem indexItem) {
        final IndexView index = indexItem.getIndex();

        final Collection<ClassInfo> aggregates =
                index.getAllKnownSubclasses(DotName.createSimple(AggregateRoot.class));

        final List<DiscoveredClassBuildItem> buildItems = new ArrayList<>();
        final Set<DotName> visited = new HashSet<>();

        for (final ClassInfo aggregate : aggregates) {
            collectFieldsRecursive(aggregate.name(), index, visited, buildItems);
        }

        return buildItems;
    }

    private void collectFieldsRecursive(final DotName type, final IndexView index, final Set<DotName> visited,
                                        final List<DiscoveredClassBuildItem> buildItems) {
        if (visited.contains(type)) {
            return;
        }
        visited.add(type);

        final ClassInfo classInfo = index.getClassByName(type);
        if (classInfo == null) {
            return;
        }

        final MethodInfo longestConstructor = classInfo.methods().stream()
                .filter(m -> m.name().equals("<init>"))
                .max(Comparator.comparingInt(m -> m.parameters().size()))
                .orElseThrow(() -> new IllegalStateException("Should not be here !"));

        // Je ne sais pas pk sur TodoChecklist uniquement la description comme field est trouvé :/
        Validate.validState(longestConstructor.parametersCount() >= classInfo.fields().size(),
                "Please add a constructor using all fields on class %s - longestConstructor parameter %d - nb of fields %d".formatted(
                        classInfo.name(),
                        longestConstructor.parametersCount(), classInfo.fields().size()));

        final List<DotName> fieldTypes = new ArrayList<>();
        final List<String> fieldNames = new ArrayList<>();

        for (int parameterIndex = 0; parameterIndex < longestConstructor.parametersCount(); parameterIndex++) {
            final Type parameterType = longestConstructor.parameterType(parameterIndex);
            final String parameterName = longestConstructor.parameterName(parameterIndex);
            fieldTypes.add(parameterType.name());
            fieldNames.add(parameterName);
            if (shouldTraverse(parameterType, index)) {
                collectFieldsRecursive(parameterType.name(), index, visited, buildItems);
            }
        }
        if (!fieldTypes.isEmpty()) {
            buildItems.add(new DiscoveredClassBuildItem(type, fieldTypes, fieldNames));
        }
    }

    private boolean shouldTraverse(final Type type, final IndexView index) {
        if (type == null) {
            return false;
        }
        // Ignore primitives
        if (type.kind() == Type.Kind.PRIMITIVE) {
            return false;
        }
        final DotName name = type.name();
        if (name == null) {
            return false;
        }
        final String nameAsString = name.toString();
        // Ignore Java standard
        if (nameAsString.startsWith("java.")
                || nameAsString.startsWith("javax.")
                || nameAsString.startsWith("jakarta.")) {
            return false;
        }
        // Ignore types
        final ClassInfo classInfo = index.getClassByName(name);
        if (classInfo.isEnum() || classInfo.isAbstract() || classInfo.isAnnotation() || classInfo.isInterface()
                || classInfo.isModule()) {
            return false;
        }
        return true;
    }

    @BuildStep
    void generateMixins(final List<DiscoveredClassBuildItem> discoveredClassBuildItems,
                        final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                        final OutputTargetBuildItem outputTargetBuildItem) {
        discoveredClassBuildItems.forEach(discovered -> {
            final String mixinClassName = discovered.getSource() + "Mixin";
            try (final ClassCreator mixinClassCreator = ClassCreator.builder()
                    .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                    .className(mixinClassName)
                    .build()) {

                // Création du constructeur @JsonCreator
                final MethodCreator constructor = mixinClassCreator.getMethodCreator("<init>", "V",
                        toTypeArray(discovered.getFieldTypes()));

                constructor.addAnnotation(JsonCreator.class);

                constructor.invokeSpecialMethod(
                        MethodDescriptor.ofMethod(Object.class, "<init>", void.class),
                        constructor.getThis());

                // Ajouter @JsonProperty sur chaque paramètre
                for (int i = 0; i < discovered.getFieldNames().size(); i++) {
                    final String fieldName = discovered.getFieldNames().get(i);
                    constructor.getParameterAnnotations(i)
                            .addAnnotation(JsonProperty.class)
                            .addValue("value", fieldName);
                }
                constructor.returnValue(null);
                CodeGenerationProcessor.writeGeneratedClass(mixinClassCreator, outputTargetBuildItem);
            }
        });
        generateMixinRegistrarObjectMapper(discoveredClassBuildItems, generatedBeanBuildItemBuildProducer, outputTargetBuildItem);
    }

    private String[] toTypeArray(final List<DotName> fields) {
        return fields.stream()
                .map(DotName::toString)
                .toArray(String[]::new);
    }

    void generateMixinRegistrarObjectMapper(final List<DiscoveredClassBuildItem> discoveredClassBuildItems,
                                            final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                            final OutputTargetBuildItem outputTargetBuildItem) {
        try (final ClassCreator beanClassCreator = ClassCreator.builder()
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                .className(MixinRegistrationObjectMapperCustomizer.class.getName() + "Generated")
                .superClass(MixinRegistrationObjectMapperCustomizer.class)
                .build()) {

            beanClassCreator.addAnnotation(Singleton.class);
            beanClassCreator.addAnnotation(Unremovable.class);

            final MethodCreator mixinsMethod = beanClassCreator.getMethodCreator("mixins", Map.class);
            mixinsMethod.setSignature(
                    SignatureBuilder.forMethod()
                            .setReturnType(parameterizedType(
                                    io.quarkus.gizmo.Type.classType(Map.class),
                                    io.quarkus.gizmo.Type.classType(String.class),
                                    io.quarkus.gizmo.Type.classType(String.class)))
                            .build());
            mixinsMethod.setModifiers(Modifier.PROTECTED);

            final ResultHandle map = mixinsMethod.newInstance(
                    MethodDescriptor.ofConstructor(HashMap.class)
            );

            for (final DiscoveredClassBuildItem discovered : discoveredClassBuildItems) {
                final String mixinClassName = discovered.getSource() + "Mixin";

                final ResultHandle sourceClass = mixinsMethod.load(discovered.getSource().toString());
                final ResultHandle mixinClass = mixinsMethod.load(mixinClassName);

                mixinsMethod.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(HashMap.class, "put", Object.class, Object.class, Object.class),
                        map, sourceClass, mixinClass
                );
            }
            mixinsMethod.returnValue(map);
            CodeGenerationProcessor.writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
        }
    }
}

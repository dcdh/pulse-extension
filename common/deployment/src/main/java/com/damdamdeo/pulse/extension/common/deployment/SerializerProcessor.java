package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.DiscoveredClassBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.EligibleTypeForSerializationBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.serialization.MixinRegistrationObjectMapperCustomizer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.Validate;
import org.jboss.jandex.*;
import org.jboss.jandex.Type;

import java.lang.reflect.Modifier;
import java.util.*;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;
import static io.quarkus.gizmo.Type.parameterizedType;

public class SerializerProcessor {

    @BuildStep
    public List<DiscoveredClassBuildItem> discoverAggregateFields(
            final List<EligibleTypeForSerializationBuildItem> eligibleTypeForSerializationBuildItems,
            final CombinedIndexBuildItem indexItem) {
        final IndexView index = indexItem.getIndex();

        final List<DiscoveredClassBuildItem> buildItems = new ArrayList<>();
        final Set<DotName> visited = new HashSet<>();
        for (EligibleTypeForSerializationBuildItem eligibleTypeForSerializationBuildItem : eligibleTypeForSerializationBuildItems) {
            if (eligibleTypeForSerializationBuildItem.clazz().isInterface()) {
                final Collection<ClassInfo> clazzes = index.getAllKnownSubclasses(DotName.createSimple(eligibleTypeForSerializationBuildItem.clazz()));
                for (final ClassInfo clazz : clazzes) {
                    collectFieldsRecursive(clazz.name(), index, visited, buildItems);
                }
            } else if (eligibleTypeForSerializationBuildItem.clazz().isLocalClass()
                    || eligibleTypeForSerializationBuildItem.clazz().isMemberClass()
                    || eligibleTypeForSerializationBuildItem.clazz().isRecord()
                    || eligibleTypeForSerializationBuildItem.clazz().isSealed()) {
                collectFieldsRecursive(DotName.createSimple(eligibleTypeForSerializationBuildItem.clazz()), index, visited, buildItems);
            }
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

        for (int i = 0; i < longestConstructor.parametersCount(); i++) {
            Type paramType = longestConstructor.parameterType(i);
            String paramName = longestConstructor.parameterName(i);

            fieldTypes.add(paramType.name());
            fieldNames.add(paramName);

            // resolve ALL possible types (List<T> -> T, Map<K,V> -> K & V)
            List<DotName> traversables = resolveTraversableAll(paramType, index);
            for (DotName t : traversables) {
                collectFieldsRecursive(t, index, visited, buildItems);
            }
        }
        if (!fieldTypes.isEmpty()) {
            buildItems.add(new DiscoveredClassBuildItem(type, fieldTypes, fieldNames));
        }
    }

    private List<DotName> resolveTraversableAll(final Type type, final IndexView index) {
        if (type == null) {
            return List.of();
        }

        // ----- Arrays : MyType[] -----
        if (type.kind() == Type.Kind.ARRAY) {
            Type component = type.asArrayType().component();
            return resolveTraversableAll(component, index);
        }

        // ----- Parameterized types -----
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType p = type.asParameterizedType();
            DotName raw = p.name();

            // ---- Collections ----
            if (isCollection(raw)) {
                if (p.arguments().size() == 1) {
                    return resolveTraversableAll(p.arguments().getFirst(), index);
                }
                return List.of();
            }

            // ---- Maps ----
            if (isMap(raw)) {
                if (p.arguments().size() == 2) {
                    Type key = p.arguments().get(0);
                    Type value = p.arguments().get(1);

                    List<DotName> result = new ArrayList<>();
                    result.addAll(resolveTraversableAll(key, index));
                    result.addAll(resolveTraversableAll(value, index));
                    return result;
                }
                return List.of();
            }
        }

        // ----- Primitives -----
        if (type.kind() == Type.Kind.PRIMITIVE) {
            return List.of();
        }

        // ----- Raw class -----
        DotName name = type.name();
        if (name == null) {
            return List.of();
        }

        String fqcn = name.toString();

        // Ignore built-in types
        if (fqcn.startsWith("javax.")
                || fqcn.startsWith("jakarta.")
                || fqcn.startsWith("java.")) {
            return List.of();
        }

        ClassInfo info = index.getClassByName(name);
        if (info == null
                || info.isEnum()
                || info.isAbstract()
                || info.isAnnotation()
                || info.isInterface()
                || info.isModule()) {
            return List.of();
        }

        return List.of(name); // ✔ traversable
    }

    private boolean isCollection(final DotName n) {
        String s = n.toString();
        return s.equals("java.util.List")
                || s.equals("java.util.Set")
                || s.equals("java.util.Collection")
                || s.equals("java.util.Queue")
                || s.equals("java.util.Deque");
    }

    private boolean isMap(final DotName n) {
        return n.toString().equals("java.util.Map");
    }

    @BuildStep
    void generateMixins(final List<DiscoveredClassBuildItem> discoveredClassBuildItems,
                        final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                        final BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemProducer,
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
                writeGeneratedClass(mixinClassCreator, outputTargetBuildItem);
            }
            reflectiveClassBuildItemProducer.produce(ReflectiveClassBuildItem.builder(mixinClassName).constructors().build());
            reflectiveClassBuildItemProducer.produce(ReflectiveClassBuildItem.builder(discovered.getSource().toString()).constructors().build());
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
            writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
        }
    }
}

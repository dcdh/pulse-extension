package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.writer.runtime.query.AggregateIdDeserializer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.Gizmo;
import org.jboss.jandex.*;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import java.util.*;

import static org.objectweb.asm.Type.getType;

public class QueryProcessor {

    private static final DotName PROJECTION = DotName.createSimple(Projection.class);
    private static final DotName AGGREGATE_ID = DotName.createSimple(AggregateId.class);

    /**
     * Generate @JsonDeserialize(using = AggregateIdDeserializer.class) to AggregateId fields in projections and nested objects
     * Sample of what we want to generate:
     * record TodoProjection(
     *
     * @JsonDeserialize(using = AggregateIdDeserializer.class)
     * TodoId todoId,
     * String description,
     * Status status,
     * boolean important,
     * List<TodoChecklistProjection> checklist) implements Projection {
     * }
     * record TodoChecklistProjection(
     * @JsonDeserialize(using = AggregateIdDeserializer.class)
     * TodoChecklistId todoChecklistId, String description) {
     * }
     */

    @BuildStep
    List<BytecodeTransformerBuildItem> transformProjectionFields(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final IndexView index = combinedIndexBuildItem.getIndex();
        final Map<DotName, Set<String>> fieldsPerClass = new HashMap<>();
        for (final ClassInfo projection : index.getAllKnownImplementations(PROJECTION)) {
            collectProjectionFields(projection, index, new HashSet<>(), fieldsPerClass);
        }
        final List<BytecodeTransformerBuildItem> transformers = new ArrayList<>();
        for (final Map.Entry<DotName, Set<String>> entry : fieldsPerClass.entrySet()) {
            final Set<String> fieldsToTransform = entry.getValue();
            if (fieldsToTransform.isEmpty()) {
                continue;
            }
            transformers.add(
                    new BytecodeTransformerBuildItem(
                            entry.getKey().toString(),
                            (className, visitor) ->
                                    new ClassVisitor(Gizmo.ASM_API_VERSION, visitor) {

                                        @Override
                                        public FieldVisitor visitField(
                                                final int access,
                                                final String fieldName,
                                                final String descriptor,
                                                final String signature,
                                                final Object value) {

                                            final FieldVisitor fv = super.visitField(access, fieldName, descriptor, signature, value);
                                            if (!fieldsToTransform.contains(fieldName)) {
                                                return fv;
                                            }
                                            return new FieldVisitor(Gizmo.ASM_API_VERSION, fv) {
                                                @Override
                                                public void visitEnd() {
                                                    final AnnotationVisitor av = fv.visitAnnotation(
                                                            "Lcom/fasterxml/jackson/databind/annotation/JsonDeserialize;",
                                                            true);
                                                    av.visit("using", getType(AggregateIdDeserializer.class));
                                                    av.visitEnd();
                                                    super.visitEnd();
                                                }
                                            };
                                        }
                                    }
                    )
            );
        }
        return transformers;
    }

    private void collectProjectionFields(
            final ClassInfo projectionClass,
            final IndexView index,
            final Set<DotName> visited,
            final Map<DotName, Set<String>> fieldsPerClass) {
        if (!visited.add(projectionClass.name())) {
            return;
        }
        final Set<String> fields = fieldsPerClass.computeIfAbsent(projectionClass.name(), ignored -> new HashSet<>());
        for (final FieldInfo field : projectionClass.fields()) {
            final Type type = field.type();
            if (isAggregateId(type, index)) {
                fields.add(field.name());
                continue;
            }
            collectNestedProjections(type, index, visited, fieldsPerClass);
        }
    }

    private void collectNestedProjections(
            final Type type,
            final IndexView index,
            final Set<DotName> visited,
            final Map<DotName, Set<String>> fieldsPerClass) {
        switch (type.kind()) {
            case CLASS -> {
                final DotName name = type.asClassType().name();
                final ClassInfo classInfo = index.getClassByName(name);
                if (classInfo != null) {
                    collectProjectionFields(classInfo, index, visited, fieldsPerClass);
                }
            }
            case PARAMETERIZED_TYPE -> {
                for (final Type argument : type.asParameterizedType().arguments()) {
                    collectNestedProjections(argument, index, visited, fieldsPerClass);
                }
            }
            case ARRAY -> collectNestedProjections(type.asArrayType().constituent(), index, visited, fieldsPerClass);
            default -> {
            }
        }
    }

    private boolean isAggregateId(final Type type, final IndexView index) {
        if (type.kind() != Type.Kind.CLASS) {
            return false;
        }
        return implementsAggregateId(type.asClassType().name(), index, new HashSet<>());
    }

    private boolean implementsAggregateId(final DotName className, final IndexView index, final Set<DotName> visited) {
        if (!visited.add(className)) {
            return false;
        }
        if (className.equals(AGGREGATE_ID)) {
            return true;
        }
        final ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            return false;
        }
        for (final DotName interfaceName : classInfo.interfaceNames()) {
            if (interfaceName.equals(AGGREGATE_ID)) {
                return true;
            }
            if (implementsAggregateId(interfaceName, index, visited)) {
                return true;
            }
        }
        final DotName superName = classInfo.superName();
        return superName != null && implementsAggregateId(superName, index, visited);
    }
}


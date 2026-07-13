package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.query.Projection;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.*;

import java.util.*;
import java.util.stream.Collectors;

public final class ValidationProcessor {

    private static final DotName PROJECTION = DotName.createSimple(Projection.class);
    private static final DotName AGGREGATE_ID = DotName.createSimple(AggregateId.class);
    private static final DotName COLLECTION = DotName.createSimple(Collection.class);
    private static final Set<DotName> COLLECTION_TYPES = Set.of(
            DotName.createSimple(Collection.class),
            DotName.createSimple(List.class),
            DotName.createSimple(Set.class),
            DotName.createSimple(Queue.class),
            DotName.createSimple(Deque.class)
    );

    @BuildStep
    List<ValidationErrorBuildItem> validateProjectionChildren(
            final CombinedIndexBuildItem combinedIndexBuildItem) {
        final IndexView index = combinedIndexBuildItem.getIndex();
        final Set<DotName> projections = index.getAllKnownImplementations(PROJECTION)
                .stream()
                .map(ClassInfo::name)
                .collect(Collectors.toSet());
        projections.add(PROJECTION);
        final Set<DotName> aggregateIds = index.getAllKnownImplementations(AGGREGATE_ID)
                .stream()
                .map(ClassInfo::name)
                .collect(Collectors.toSet());
        aggregateIds.add(AGGREGATE_ID);
        final List<ValidationErrorBuildItem> errors = new ArrayList<>();
        for (final ClassInfo projection : index.getAllKnownImplementations(PROJECTION)) {
            validateProjection(
                    index,
                    projections,
                    aggregateIds,
                    projection,
                    new HashSet<>(),
                    projection.name().toString(),
                    errors
            );
        }
        return errors;
    }

    private void validateProjection(
            final IndexView index,
            final Set<DotName> projections,
            final Set<DotName> aggregateIds,
            final ClassInfo projection,
            final Set<DotName> visited,
            final String path,
            final List<ValidationErrorBuildItem> errors) {
        if (!visited.add(projection.name())) {
            return;
        }

        /*
         * Support des records
         */
        if (projection.isRecord()) {
            for (final RecordComponentInfo component : projection.recordComponents()) {
                validateType(
                        index,
                        projections,
                        aggregateIds,
                        component.type(),
                        visited,
                        path + "." + component.name(),
                        errors
                );
            }
            return;
        }

        /*
         * Support classes classiques
         */
        for (final FieldInfo field : projection.fields()) {
            validateType(
                    index,
                    projections,
                    aggregateIds,
                    field.type(),
                    visited,
                    path + "." + field.name(),
                    errors
            );
        }
    }

    private void validateType(
            final IndexView index,
            final Set<DotName> projections,
            final Set<DotName> aggregateIds,
            final Type type,
            final Set<DotName> visited,
            final String path,
            final List<ValidationErrorBuildItem> errors) {
        switch (type.kind()) {
            case PRIMITIVE:
                return;
            case ARRAY:
                validateValueType(
                        index,
                        projections,
                        aggregateIds,
                        type.asArrayType().constituent(),
                        visited,
                        path + "[]",
                        errors
                );
                return;
            case PARAMETERIZED_TYPE:
                final ParameterizedType parameterizedType = type.asParameterizedType();
                /*
                 * Collection<T>
                 */
                if (isCollection(index, parameterizedType.name())) {
                    if (!parameterizedType.arguments().isEmpty()) {
                        validateValueType(
                                index,
                                projections,
                                aggregateIds,
                                parameterizedType.arguments().getFirst(),
                                visited,
                                path,
                                errors
                        );
                    }
                    return;
                }
                /*
                 * Autres types génériques non supportés
                 */
                validateValueType(
                        index,
                        projections,
                        aggregateIds,
                        type,
                        visited,
                        path,
                        errors
                );
                return;
            case CLASS:
                validateValueType(
                        index,
                        projections,
                        aggregateIds,
                        type,
                        visited,
                        path,
                        errors
                );
                return;
            default:
                return;
        }
    }

    private void validateValueType(
            final IndexView index,
            final Set<DotName> projections,
            final Set<DotName> aggregateIds,
            final Type type,
            final Set<DotName> visited,
            final String path,
            final List<ValidationErrorBuildItem> errors) {
        if (type.kind() != Type.Kind.CLASS) {
            return;
        }
        final DotName name = type.name();

        /*
         * java.lang.*
         */
        if (name.toString().startsWith("java.lang.")) {
            return;
        }

        final ClassInfo classInfo = index.getClassByName(name);

        /*
         * enum
         */
        if (classInfo != null && classInfo.isEnum()) {
            return;
        }

        /*
         * AggregateId
         */
        if (aggregateIds.contains(name)) {
            return;
        }

        /*
         * Projection
         */
        if (projections.contains(name)) {
            if (classInfo != null) {
                validateProjection(
                        index,
                        projections,
                        aggregateIds,
                        classInfo,
                        visited,
                        path,
                        errors
                );
            }
            return;
        }
        errors.add(
                new ValidationErrorBuildItem(
                        new IllegalStateException(
                                "Invalid projection property '" + path
                                        + "'. Type '"
                                        + name
                                        + "' must implement Projection or AggregateId."
                        )
                )
        );
    }

    private boolean isCollection(
            final IndexView index,
            final DotName type) {
        if (COLLECTION_TYPES.contains(type)) {
            return true;
        }
        final ClassInfo classInfo = index.getClassByName(type);
        if (classInfo == null) {
            return false;
        }
        return implementsInterface(
                index,
                classInfo,
                COLLECTION,
                new HashSet<>()
        );
    }

    private boolean implementsInterface(
            final IndexView index,
            final ClassInfo classInfo,
            final DotName expected,
            final Set<DotName> visited) {
        if (!visited.add(classInfo.name())) {
            return false;
        }
        for (final Type interfaceType : classInfo.interfaceTypes()) {
            if (interfaceType.name().equals(expected)) {
                return true;
            }
            final ClassInfo interfaceInfo =
                    index.getClassByName(interfaceType.name());
            if (interfaceInfo != null &&
                    implementsInterface(
                            index,
                            interfaceInfo,
                            expected,
                            visited)) {
                return true;
            }
        }
        final Type superClass = classInfo.superClassType();
        if (superClass != null) {
            final ClassInfo superInfo = index.getClassByName(superClass.name());
            if (superInfo != null &&
                    implementsInterface(
                            index,
                            superInfo,
                            expected,
                            visited)) {
                return true;
            }
        }
        return false;
    }
}

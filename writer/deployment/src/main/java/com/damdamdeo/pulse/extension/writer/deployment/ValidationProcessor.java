package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.Event;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.ClassInfo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ValidationProcessor {

    // TODO tester empty constructor

    @BuildStep
    List<ValidationErrorBuildItem> validateAggregateIdImplementationIsARecord(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(AggregateId.class)
                .stream()
                .filter(Predicate.not(ClassInfo::isRecord))
                .map(invalidClazz -> new ValidationErrorBuildItem(
                        new IllegalStateException("AggregateId '%s' must be a record".formatted(invalidClazz.name()))))
                .toList();
    }

    @BuildStep
    List<ValidationErrorBuildItem> validateEventImplementationIsARecord(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Event.class)
                .stream()
                .filter(Predicate.not(ClassInfo::isRecord))
                .map(invalidClazz -> new ValidationErrorBuildItem(
                        new IllegalStateException("Event '%s' must be a record".formatted(invalidClazz.name()))))
                .toList();
    }

    @BuildStep
    List<ValidationErrorBuildItem> validateCommandImplementationIsARecord(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Command.class)
                .stream()
                .filter(Predicate.not(ClassInfo::isRecord))
                .map(invalidClazz -> new ValidationErrorBuildItem(
                        new IllegalStateException("Command '%s' must be a record".formatted(invalidClazz.name()))))
                .toList();
    }

    @BuildStep
    List<ValidationErrorBuildItem> validateAggregateRootNameUniqueness(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final Map<String, Long> countsByClassSimpleName = combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(AggregateRoot.class)
                .stream()
                .map(ClassInfo::simpleName)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return countsByClassSimpleName.entrySet()
                .stream()
                .filter(e -> e.getValue() > 1L)
                .map(e -> new ValidationErrorBuildItem(
                        new IllegalArgumentException("AggregateRoot '%s' declared more than once '%d'"
                                .formatted(e.getKey(), e.getValue()))))
                .toList();
    }

    @BuildStep
    List<ValidationErrorBuildItem> validateEventNameUniqueness(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final Map<String, Long> countsByClassSimpleName = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Event.class)
                .stream()
                .map(ClassInfo::simpleName)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return countsByClassSimpleName.entrySet()
                .stream()
                .filter(e -> e.getValue() > 1L)
                .map(e -> new ValidationErrorBuildItem(
                        new IllegalArgumentException("Event '%s' declared more than once '%d'"
                                .formatted(e.getKey(), e.getValue()))))
                .toList();
    }
}

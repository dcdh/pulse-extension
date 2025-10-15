package com.damdamdeo.pulse.extension.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.deployment.items.ValidationErrorBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.ClassInfo;

import java.util.List;
import java.util.function.Predicate;

public class ValidationProcessor {

    // TODO tester empty constructor

    @BuildStep
    void mapToQuarkusValidationErrorBuildItem(final List<ValidationErrorBuildItem> validationErrorBuildItems,
                                              final BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrorBuildItemProducer) {
        if (!validationErrorBuildItems.isEmpty()) {
            validationErrorBuildItemProducer.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    validationErrorBuildItems.stream().map(ValidationErrorBuildItem::getCause).toList()));
        }
    }

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
}

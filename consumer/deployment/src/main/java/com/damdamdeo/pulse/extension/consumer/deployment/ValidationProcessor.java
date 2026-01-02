package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.consumer.deployment.items.ConsumerChannelToValidateBuildItem;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValidationProcessor {

    record InvalidNaming(String naming, Pattern pattern, Kind kind) implements InvalidMessage {

        enum Kind {
            TARGET,
            FUNCTIONAL_NAME,
            COMPONENT_NAME
        }

        public InvalidNaming {
            Objects.requireNonNull(naming);
            Objects.requireNonNull(pattern);
            Objects.requireNonNull(kind);
        }

        public static InvalidNaming ofTarget(final String naming, final Pattern pattern) {
            return new InvalidNaming(naming, pattern, Kind.TARGET);
        }

        public static InvalidNaming ofFunctional(final String naming, final Pattern pattern) {
            return new InvalidNaming(naming, pattern, Kind.FUNCTIONAL_NAME);
        }

        public static InvalidNaming ofComponent(final String naming, final Pattern pattern) {
            return new InvalidNaming(naming, pattern, Kind.COMPONENT_NAME);
        }

        @Override
        public String invalidMessage() {
            return switch (kind) {
                case Kind.TARGET ->
                        "Invalid Target name '%s' - it should match '%s'".formatted(naming, pattern.pattern());
                case Kind.FUNCTIONAL_NAME ->
                        "Invalid Functional name '%s' - it should match '%s'".formatted(naming, pattern.pattern());
                case Kind.COMPONENT_NAME ->
                        "Invalid Component name '%s' - it should match '%s'".formatted(naming, pattern.pattern());
            };
        }
    }

    interface InvalidMessage {

        String invalidMessage();
    }

    interface InvalidDuplicates extends InvalidMessage {

    }

    record InvalidTargetDuplicates(String naming, Long count) implements InvalidDuplicates {

        InvalidTargetDuplicates {
            Objects.requireNonNull(naming);
            Objects.requireNonNull(count);
        }

        @Override
        public String invalidMessage() {
            return "Target '%s' declared more than once '%d'".formatted(naming, count);
        }
    }

    record InvalidSourceDuplicates(String target, String functionalDomain, String componentName,
                                   Long count) implements InvalidDuplicates {

        InvalidSourceDuplicates {
            Objects.requireNonNull(target);
            Objects.requireNonNull(functionalDomain);
            Objects.requireNonNull(componentName);
            Objects.requireNonNull(count);
        }

        @Override
        public String invalidMessage() {
            return "functionalDomain '%s' componentName '%s' declared more than once '%d' in purpose '%s'"
                    .formatted(functionalDomain, componentName, count, target);
        }
    }

    record PreValidation(List<PreValidationEventChannel> preValidationEventChannels) {

        PreValidation {
            Objects.requireNonNull(preValidationEventChannels);
        }

        public List<InvalidNaming> invalidNamings() {
            final List<InvalidNaming> invalidNamings = new ArrayList<>();
            for (final PreValidationEventChannel preValidationEventChannel : preValidationEventChannels) {
                if (!Purpose.TARGET_PATTERN.matcher(preValidationEventChannel.purpose()).matches()) {
                    invalidNamings.add(InvalidNaming.ofTarget(preValidationEventChannel.purpose(), Purpose.TARGET_PATTERN));
                }
                for (final PreValidationSource preValidationSource : preValidationEventChannel.sources()) {
                    if (!FromApplication.PART_PATTERN.matcher(preValidationSource.functionalDomain()).matches()) {
                        invalidNamings.add(InvalidNaming.ofFunctional(preValidationSource.functionalDomain(), FromApplication.PART_PATTERN));
                    }
                    if (!FromApplication.PART_PATTERN.matcher(preValidationSource.componentName()).matches()) {
                        invalidNamings.add(InvalidNaming.ofComponent(preValidationSource.componentName(), FromApplication.PART_PATTERN));
                    }
                }
            }
            return invalidNamings;
        }

        public List<InvalidDuplicates> invalidDuplicates() {
            final List<InvalidDuplicates> invalidDuplicates = new ArrayList<>();

            preValidationEventChannels
                    .stream()
                    .map(PreValidationEventChannel::purpose)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream()
                    .filter(tc -> tc.getValue() > 1L)
                    .forEach(tc -> invalidDuplicates.add(
                            new InvalidTargetDuplicates(tc.getKey(), tc.getValue())));

            for (final PreValidationEventChannel preValidationEventChannel : preValidationEventChannels) {
                final String target = preValidationEventChannel.purpose();
                preValidationEventChannel.sources().stream()
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                        .entrySet().stream()
                        .filter(tc -> tc.getValue() > 1L)
                        .forEach(tc -> invalidDuplicates.add(
                                new InvalidSourceDuplicates(target, tc.getKey().functionalDomain(), tc.getKey().componentName(), tc.getValue())));
            }
            return invalidDuplicates;
        }
    }

    record PreValidationEventChannel(String purpose, List<PreValidationSource> sources) {

        PreValidationEventChannel {
            Objects.requireNonNull(purpose);
            Objects.requireNonNull(sources);
        }
    }

    record PreValidationSource(String functionalDomain, String componentName) {

        PreValidationSource {
            Objects.requireNonNull(functionalDomain);
            Objects.requireNonNull(componentName);
        }
    }

    @BuildStep
    List<ValidationErrorBuildItem> validate(
            final List<ConsumerChannelToValidateBuildItem> consumerChannelToValidateBuildItems,
            final CombinedIndexBuildItem combinedIndexBuildItem) {
        final IndexView index = combinedIndexBuildItem.getIndex();
        final List<PreValidationEventChannel> preValidationEventChannels =
                consumerChannelToValidateBuildItems.stream()
                        .flatMap(item -> index.getAnnotations(item.clazz()).stream())
                        .map(asyncConsumerChannel -> {
                            final String target = asyncConsumerChannel.value("purpose").asString();

                            final List<PreValidationSource> sources =
                                    asyncConsumerChannel.value("sources")
                                            .asArrayList()
                                            .stream()
                                            .map(source -> {
                                                final AnnotationInstance nested = source.asNested();
                                                return new PreValidationSource(
                                                        nested.value("functionalDomain").asString(),
                                                        nested.value("componentName").asString());
                                            })
                                            .toList();

                            return new PreValidationEventChannel(target, sources);
                        })
                        .toList();

        final PreValidation preValidation = new PreValidation(preValidationEventChannels);

        return Stream.concat(
                        preValidation.invalidNamings().stream(),
                        preValidation.invalidDuplicates().stream())
                .map(invalidMessage ->
                        new ValidationErrorBuildItem(
                                new IllegalArgumentException(
                                        invalidMessage.invalidMessage())))
                .toList();
    }
}

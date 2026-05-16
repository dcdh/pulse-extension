package com.damdamdeo.pulse.extension.core;

import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.function.Function;

public class AggregateIdGenerator {

    private final SequenceGenerator sequenceGenerator;

    public AggregateIdGenerator(final SequenceGenerator sequenceGenerator) {
        this.sequenceGenerator = Objects.requireNonNull(sequenceGenerator);
    }

    public <A extends AggregateId> A generate(final Class<A> clazz, final Function<SequenceNumber, A> creational) throws SequenceGenerationException {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(creational);
        final A generated = creational.apply(sequenceGenerator.nextFor(clazz));
        Validate.matchesPattern(generated.id(), "^[a-zA-Z]+-[A-Z0-9\\-]+$");
        return generated;
    }

    public <A extends AggregateId> A generate(final For<A> identifiable, final Function<SequenceNumber, A> creational) throws SequenceGenerationException {
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(creational);
        final A generated = creational.apply(sequenceGenerator.nextFor(identifiable));
        Validate.matchesPattern(generated.id(), "^[a-zA-Z]+-[A-Z0-9\\-]+$");
        return generated;
    }
}

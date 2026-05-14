package com.damdamdeo.pulse.extension.core;

public interface SequenceGenerator {

    <A extends AggregateId> SequenceNumber nextFor(Class<A> aggregateIdClazz) throws SequenceGenerationException;
}

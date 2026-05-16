package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

public interface SequenceGenerator {

    <A extends Identifiable> SequenceNumber nextFor(Class<A> identifiableClazz) throws SequenceGenerationException;
}

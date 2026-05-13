package com.damdamdeo.pulse.extension.core.hashing;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

public interface Hasher {

    <T extends Identifiable> Hash<T> hash(T identifiable);
}

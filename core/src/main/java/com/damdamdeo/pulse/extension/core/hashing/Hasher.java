package com.damdamdeo.pulse.extension.core.hashing;

public interface Hasher {

    Hash hash(Algorithm algorithm, String original);
}

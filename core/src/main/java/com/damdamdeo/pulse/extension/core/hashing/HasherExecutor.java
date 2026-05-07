package com.damdamdeo.pulse.extension.core.hashing;

public interface HasherExecutor {

    Hash hash(Algorithm algorithm, byte[] original);
}

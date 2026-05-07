package com.damdamdeo.pulse.extension.core.hashing;

public interface HasherExecutor {

    Hash hash(final Algorithm algorithm, final byte[] original);
}

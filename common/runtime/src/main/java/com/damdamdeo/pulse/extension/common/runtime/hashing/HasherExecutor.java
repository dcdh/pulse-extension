package com.damdamdeo.pulse.extension.common.runtime.hashing;

import com.damdamdeo.pulse.extension.core.hashing.Algorithm;
import com.damdamdeo.pulse.extension.core.hashing.Hash;

public interface HasherExecutor {

    Hash hash(final Algorithm algorithm, final byte[] original);
}

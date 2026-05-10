package com.damdamdeo.pulse.extension.common.runtime.hashing;

import com.damdamdeo.pulse.extension.core.hashing.Hash;

public interface InternalHasher {

    Hash hash(String original);
}

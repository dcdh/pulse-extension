package com.damdamdeo.pulse.extension.common.runtime.hashing;

import com.damdamdeo.pulse.extension.core.hashing.Hash;

public interface HasherImplementation {

    Hash hash(String original);
}

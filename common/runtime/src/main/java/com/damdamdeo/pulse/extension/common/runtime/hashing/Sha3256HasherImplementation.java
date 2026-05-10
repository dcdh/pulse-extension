package com.damdamdeo.pulse.extension.common.runtime.hashing;

import com.damdamdeo.pulse.extension.core.hashing.Algorithm;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.codec.digest.DigestUtils;

@ApplicationScoped
@Unremovable
@DefaultBean
@AlgorithmQualifier(Algorithm.SHA3_256)
public class Sha3256HasherImplementation implements HasherImplementation {

    @Override
    public Hash hash(final String original) {
        return new Hash(Algorithm.SHA3_256, new DigestUtils("SHA3-256").digestAsHex(original));
    }
}

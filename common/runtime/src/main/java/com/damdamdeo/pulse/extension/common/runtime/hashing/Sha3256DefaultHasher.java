package com.damdamdeo.pulse.extension.common.runtime.hashing;

import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.codec.digest.DigestUtils;

@ApplicationScoped
@Unremovable
@DefaultBean
public class Sha3256DefaultHasher implements Hasher {

    public Hash hash(final String original) {
        return new Hash(new DigestUtils("SHA3-256").digestAsHex(original));
    }
}

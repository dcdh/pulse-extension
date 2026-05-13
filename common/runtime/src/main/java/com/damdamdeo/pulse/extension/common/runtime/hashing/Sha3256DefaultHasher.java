package com.damdamdeo.pulse.extension.common.runtime.hashing;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Objects;

@ApplicationScoped
@Unremovable
@DefaultBean
public class Sha3256DefaultHasher implements Hasher {

    public <T extends Identifiable> Hash<T> hash(final T identifiable) {
        Objects.requireNonNull(identifiable);
        return new Hash<T>(new DigestUtils("SHA3-256").digestAsHex(identifiable.id()));
    }
}

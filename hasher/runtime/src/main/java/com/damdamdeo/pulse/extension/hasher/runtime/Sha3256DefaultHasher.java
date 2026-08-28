package com.damdamdeo.pulse.extension.hasher.runtime;

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

    private final HasherConfig hasherConfig;

    public Sha3256DefaultHasher(final HasherConfig hasherConfig) {
        this.hasherConfig = Objects.requireNonNull(hasherConfig);
    }

    @Override
    public <T extends Identifiable> Hash<T> hash(final T identifiable) {
        Objects.requireNonNull(identifiable);
        return new Hash<>(hash(identifiable.id()));
    }

    @Override
    public String hash(final String value) {
        Objects.requireNonNull(value);
        return hasherConfig.pepper().map(pepper -> execute(value + ":" + pepper))
                .orElseGet(() -> execute(value));
    }

    private String execute(final String value) {
        return new DigestUtils("SHA3-256").digestAsHex(value);
    }
}

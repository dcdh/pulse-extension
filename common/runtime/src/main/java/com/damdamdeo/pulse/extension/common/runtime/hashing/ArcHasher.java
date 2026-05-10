package com.damdamdeo.pulse.extension.common.runtime.hashing;

import com.damdamdeo.pulse.extension.core.hashing.Algorithm;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Objects;

@ApplicationScoped
@Unremovable
public class ArcHasher implements Hasher {

    @Inject
    @Any
    Instance<HasherImplementation> hashersImplementation;

    @Override
    public Hash hash(final Algorithm algorithm, final String original) {
        Objects.requireNonNull(algorithm);
        Objects.requireNonNull(original);
        return hashersImplementation.select(new AlgorithmQualifier.Literal(algorithm)).get().hash(original);
    }
}

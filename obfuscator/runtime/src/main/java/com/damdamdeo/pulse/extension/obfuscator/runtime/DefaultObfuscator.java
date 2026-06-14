package com.damdamdeo.pulse.extension.obfuscator.runtime;

import com.damdamdeo.pulse.extension.core.obfuscator.Obfuscator;
import com.damdamdeo.pulse.extension.core.obfuscator.UnableToDeObfuscateException;
import com.damdamdeo.pulse.extension.core.obfuscator.UnableToObfuscateException;
import com.damdamdeo.pulse.extension.core.obfuscator.UnknownObfuscatedException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
@Unremovable
public class DefaultObfuscator implements Obfuscator {

    private final ObfuscatorRepository obfuscatorRepository;
    private final UUIDProvider uuidProvider;

    public DefaultObfuscator(final ObfuscatorRepository obfuscatorRepository, final UUIDProvider uuidProvider) {
        this.obfuscatorRepository = Objects.requireNonNull(obfuscatorRepository);
        this.uuidProvider = Objects.requireNonNull(uuidProvider);
    }

    @Override
    public String obfuscate(final String value) throws UnableToObfuscateException {
        final UUID obfuscated = uuidProvider.provide();
        return obfuscatorRepository.store(obfuscated, value).toString();
    }

    @Override
    public String deObfuscate(final String obfuscated) throws UnableToDeObfuscateException, UnknownObfuscatedException {
        return obfuscatorRepository.retrieve(UUID.fromString(obfuscated)).orElseThrow(UnknownObfuscatedException::new);
    }
}

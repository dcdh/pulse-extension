package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseGenerator;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class DefaultPassphraseProvider implements PassphraseProvider {

    private final PassphraseRepository passphraseRepository;
    private final PassphraseGenerator passphraseGenerator;

    public DefaultPassphraseProvider(final PassphraseRepository passphraseRepository,
                                     final PassphraseGenerator passphraseGenerator) {
        this.passphraseRepository = Objects.requireNonNull(passphraseRepository);
        this.passphraseGenerator = Objects.requireNonNull(passphraseGenerator);
    }

    // https://github.com/quarkusio/quarkus/issues/19676
    @CacheResult(cacheName = "passphrase")
    @Override
    public Passphrase provide(final OwnedBy ownedBy) {
        Objects.requireNonNull(ownedBy);
        return passphraseRepository.retrieve(ownedBy)
                .orElseGet(() -> {
                    final Passphrase generated = passphraseGenerator.generate();
                    passphraseRepository.store(ownedBy, generated);
                    return generated;
                });
    }
}

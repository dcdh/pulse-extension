package com.damdamdeo.pulse.extension.encryption.storage.runtime.vault;

import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.Objects;
import java.util.logging.Logger;

@ApplicationScoped
@DefaultBean
@Unremovable
public class DefaultPassphraseObfuscator implements PassphraseObfuscator {

    final Logger LOGGER = Logger.getLogger(DefaultPassphraseObfuscator.class.getName());

    @Override
    public Passphrase obfuscate(final Passphrase passphrase) {
        return Objects.requireNonNull(passphrase);
    }

    void onStart(@Observes final StartupEvent startupEvent) {
        LOGGER.warning("Using default passphrase obfuscator");
    }
}

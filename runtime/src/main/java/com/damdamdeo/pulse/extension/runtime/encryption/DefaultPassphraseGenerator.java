package com.damdamdeo.pulse.extension.runtime.encryption;

import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseGenerator;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class DefaultPassphraseGenerator implements PassphraseGenerator {

    @Override
    public Passphrase generate() {
        final SecureRandom random = new SecureRandom();
        final int length = 32; // longueur de la passphrase
        final char[] passphrase = new char[length];

        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";

        for (int i = 0; i < length; i++) {
            final int index = random.nextInt(chars.length());
            passphrase[i] = chars.charAt(index);
        }

        return new Passphrase(passphrase);
    }
}

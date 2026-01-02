package com.damdamdeo.pulse.extension.common.runtime.encryption;

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

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";

    // Never static ! not supported by GraalVM
    // Caused by: com.oracle.graal.pointsto.constraints.UnsupportedFeatureException: Detected an instance of Random/SplittableRandom class in the image heap. Instances created during image generation have cached seed values and don't behave as expected. If these objects should not be stored in the image heap, you can use
    private final SecureRandom RANDOM = new SecureRandom();

    private static final int LENGTH = 32;

    @Override
    public Passphrase generate() {
        final char[] passphrase = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            final int index = RANDOM.nextInt(CHARS.length());
            passphrase[i] = CHARS.charAt(index);
        }

        return new Passphrase(passphrase);
    }
}

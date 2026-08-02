package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoder;
import com.damdamdeo.pulse.extension.core.executedby.UnableToEncodeException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class DefaultExecutedByEncoder implements ExecutedByEncoder {

    @Inject
    PassphraseProvider passphraseProvider;

    @Inject
    EncryptionService encryptionService;

    @Override
    public Encrypted<byte[]> encode(final String value, final OwnedBy ownedBy) throws UnableToEncodeException {
        Objects.requireNonNull(value);
        Objects.requireNonNull(ownedBy);
        try {
            final Passphrase passphrase = passphraseProvider.provide(ownedBy);
            final InputStream clearData = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
            return encryptionService.encrypt(clearData, passphrase, encryptedPayload -> {
                try (final InputStream payload = encryptedPayload.payload()) {
                    return new Encrypted<>(Base64.encode(payload.readAllBytes()));
                }
            });
        } catch (final UnableToProvidePassphraseException | EncryptionException exception) {
            throw new UnableToEncodeException(exception);
        }
    }
}

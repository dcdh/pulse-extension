package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.UnableToEncodeException;
import com.damdamdeo.pulse.extension.core.executedby.UsernameEncoder;
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
public class DefaultUsernameEncoder implements UsernameEncoder {

    @Inject
    PassphraseProvider passphraseProvider;

    @Inject
    EncryptionService encryptionService;

    @Override
    public UsernameEncoded encode(final Username username, final OwnedBy ownedBy) throws UnableToEncodeException {
        Objects.requireNonNull(username);
        Objects.requireNonNull(ownedBy);
        try {
            final Passphrase passphrase = passphraseProvider.provide(ownedBy);
            final InputStream clearData = new ByteArrayInputStream(username.username().getBytes(StandardCharsets.UTF_8));
            final Encrypted<byte[]> encrypted = encryptionService.encrypt(clearData, passphrase, encryptedPayload -> {
                try (final InputStream payload = encryptedPayload.payload()) {
                    return Encrypted.of(Base64.encode(payload.readAllBytes()));
                }
            });
            return new UsernameEncoded(new String(encrypted.payload()));
        } catch (final UnableToProvidePassphraseException | EncryptionException exception) {
            throw new UnableToEncodeException(exception);
        }
    }
}

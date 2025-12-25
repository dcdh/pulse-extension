package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoder;
import com.damdamdeo.pulse.extension.core.executedby.OwnedByExecutedByEncoder;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@ApplicationScoped
@Unremovable
@DefaultBean
public class DefaultOwnedByExecutedByEncoder implements OwnedByExecutedByEncoder {

    @Inject
    PassphraseProvider passphraseProvider;

    @Inject
    EncryptionService encryptionService;

    @Override
    public ExecutedByEncoder executedByEncoder(final OwnedBy ownedBy) {
        Objects.requireNonNull(ownedBy);
        return value -> {
            Objects.requireNonNull(value);
            final Passphrase passphrase = passphraseProvider.provide(ownedBy);
            return Base64.encode(encryptionService.encrypt(value.getBytes(StandardCharsets.UTF_8), passphrase).payload());
        };
    }
}

package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.encryption.Decrypted;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionException;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByDecoder;
import com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bouncycastle.util.encoders.Base64;

import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@Unremovable
@DefaultBean
public class DefaultExecutedByDecoder implements ExecutedByDecoder {

    @Inject
    DecryptionService decryptionService;

    @Override
    public Optional<String> decode(final String encoded, final OwnedBy ownedBy) throws UnableToDecodeException {
        Objects.requireNonNull(encoded);
        Objects.requireNonNull(encoded);
        try {
            final Decrypted<byte[]> decrypted = decryptionService.decrypt(new Encrypted<>(Base64.decode(encoded)), ownedBy);
            return Optional.of(new String(decrypted.payload()));
        } catch (final DecryptionException decryptionException) {
            throw new UnableToDecodeException(decryptionException);
        }
    }
}

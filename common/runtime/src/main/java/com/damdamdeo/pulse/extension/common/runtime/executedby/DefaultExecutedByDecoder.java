package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.encryption.*;
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
            final DecryptedPayload decrypted = decryptionService.decrypt(new EncryptedPayload(Base64.decode(encoded)), ownedBy);
            return Optional.of(new String(decrypted.payload()));
        } catch (final DecryptionException decryptionException) {
            throw new RuntimeException(decryptionException);
        } catch (final UnknownPassphraseException unknownPassphraseException) {
            return Optional.empty();
        } catch (final UnableToRetrievePassphraseException unableToRetrievePassphraseException) {
            throw new UnableToDecodeException(unableToRetrievePassphraseException);
        }
    }
}

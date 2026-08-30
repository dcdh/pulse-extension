package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.encryption.Decrypted;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionException;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException;
import com.damdamdeo.pulse.extension.core.executedby.UsernameDecoder;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bouncycastle.util.encoders.Base64;

import java.util.Objects;

// TODO move in core
@ApplicationScoped
@Unremovable
@DefaultBean
public class DefaultUsernameDecoder implements UsernameDecoder {

    @Inject
    DecryptionService decryptionService;

    @Override
    public Username decode(final UsernameEncoded usernameEncoded, final OwnedBy ownedBy) throws UnableToDecodeException {
        Objects.requireNonNull(usernameEncoded);
        Objects.requireNonNull(ownedBy);
        try {
            final Decrypted<byte[]> decrypted = decryptionService.decrypt(Encrypted.of(Base64.decode(usernameEncoded.encoded())), ownedBy);
            return new Username(new String(decrypted.payload()));
        } catch (final DecryptionException decryptionException) {
            throw new UnableToDecodeException(decryptionException);
        }
    }
}

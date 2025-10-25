package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface DecryptionService {

    DecryptedPayload decrypt(EncryptedPayload encrypted, OwnedBy ownedBy) throws DecryptionException;
}

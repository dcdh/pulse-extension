package com.damdamdeo.pulse.extension.runtime.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface DecryptionService {

    byte[] decrypt(byte[] encrypted, OwnedBy ownedBy) throws DecryptionException;
}

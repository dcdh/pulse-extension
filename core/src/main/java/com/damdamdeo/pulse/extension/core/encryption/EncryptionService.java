package com.damdamdeo.pulse.extension.core.encryption;

public interface EncryptionService {

    EncryptedPayload encrypt(final byte[] clearData, final Passphrase passphrase) throws EncryptionException;
}

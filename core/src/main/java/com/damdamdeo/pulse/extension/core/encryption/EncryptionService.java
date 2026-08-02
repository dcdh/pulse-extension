package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.io.InputStream;

public interface EncryptionService {

    <T> Encrypted<T> encrypt(InputStream clearData, OwnedBy ownedBy,
                             EncryptedInputStreamMapper<Encrypted<T>> mapper) throws EncryptionException;

    <T> Encrypted<T> encrypt(InputStream clearData, Passphrase passphrase,
                             EncryptedInputStreamMapper<Encrypted<T>> mapper) throws EncryptionException;
}

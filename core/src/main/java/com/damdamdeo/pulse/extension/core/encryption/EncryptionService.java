package com.damdamdeo.pulse.extension.core.encryption;

import java.io.InputStream;

public interface EncryptionService {

    <T> Encrypted<T> encrypt(InputStream clearData, Passphrase passphrase,
                             EncryptedInputStreamMapper<Encrypted<T>> mapper) throws EncryptionException;
}

package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.io.InputStream;

public interface DecryptionService {

    <T> Decrypted<T> decrypt(Encrypted<InputStream> encrypted, OwnedBy ownedBy,
                             DecryptedInputStreamMapper<Decrypted<T>> mapper) throws DecryptionException;

    <T> Decrypted<T> decrypt(Encrypted<InputStream> encrypted, Passphrase passphrase,
                             DecryptedInputStreamMapper<Decrypted<T>> mapper) throws DecryptionException;

    Decrypted<byte[]> decrypt(Encrypted<byte[]> encrypted, OwnedBy ownedBy) throws DecryptionException;

    Decrypted<byte[]> decrypt(Encrypted<byte[]> encrypted, Passphrase passphrase) throws DecryptionException;
}

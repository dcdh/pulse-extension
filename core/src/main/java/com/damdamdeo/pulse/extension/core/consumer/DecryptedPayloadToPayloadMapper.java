package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.encryption.Decrypted;

import java.io.IOException;

public interface DecryptedPayloadToPayloadMapper<T> {

    T map(Decrypted<byte[]> decrypted) throws IOException;
}

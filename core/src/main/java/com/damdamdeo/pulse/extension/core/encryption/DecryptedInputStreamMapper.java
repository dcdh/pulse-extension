package com.damdamdeo.pulse.extension.core.encryption;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface DecryptedInputStreamMapper<T> {

    T process(Decrypted<InputStream> decrypted) throws IOException;
}

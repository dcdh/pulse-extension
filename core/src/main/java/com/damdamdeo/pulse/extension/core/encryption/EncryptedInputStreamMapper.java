package com.damdamdeo.pulse.extension.core.encryption;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface EncryptedInputStreamMapper<T> {

    T process(Encrypted<InputStream> encrypted) throws IOException;
}

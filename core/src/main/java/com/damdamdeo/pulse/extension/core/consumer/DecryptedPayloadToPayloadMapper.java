package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.encryption.DecryptedPayload;

import java.io.IOException;

public interface DecryptedPayloadToPayloadMapper<T> {

    T map(DecryptedPayload decryptedPayload) throws IOException;
}

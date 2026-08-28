package com.damdamdeo.pulse.extension.core.query.file;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public record EncryptedCustomMetadata(Encrypted<byte[]> encrypted, OwnedBy ownedBy) {

    public EncryptedCustomMetadata {
        Objects.requireNonNull(encrypted);
        Objects.requireNonNull(ownedBy);
    }
}

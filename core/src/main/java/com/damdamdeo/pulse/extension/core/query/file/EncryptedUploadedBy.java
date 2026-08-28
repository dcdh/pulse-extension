package com.damdamdeo.pulse.extension.core.query.file;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;

import java.util.Objects;

public record EncryptedUploadedBy(ExecutedByEncoded executedByEncoded, OwnedBy ownedBy) {

    public EncryptedUploadedBy {
        Objects.requireNonNull(executedByEncoded);
        Objects.requireNonNull(ownedBy);
    }
}

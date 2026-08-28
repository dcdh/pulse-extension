package com.damdamdeo.pulse.extension.core.query.file.traceability;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;

import java.util.Objects;

public record EncryptedDownloadedBy(ExecutedByEncoded executedByEncoded, OwnedBy ownedBy) {

    public EncryptedDownloadedBy {
        Objects.requireNonNull(executedByEncoded);
        Objects.requireNonNull(ownedBy);
    }
}

package com.damdamdeo.pulse.extension.core.executedby;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record ExecutedByEncoded(String encoded) {

    public ExecutedByEncoded {
        Objects.requireNonNull(encoded);
        Validate.validState(ExecutedBy.Anonymous.DISCRIMINANT.equals(encoded)
                || encoded.startsWith(ExecutedBy.EndUser.DISCRIMINANT + ExecutedBy.EndUser.SEPARATOR)
                || encoded.startsWith(ExecutedBy.ServiceAccount.DISCRIMINANT + ExecutedBy.ServiceAccount.SEPARATOR)
                || ExecutedBy.NotAvailable.DISCRIMINANT.equals(encoded));
    }
}

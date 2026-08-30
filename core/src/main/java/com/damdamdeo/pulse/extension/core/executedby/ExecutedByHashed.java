package com.damdamdeo.pulse.extension.core.executedby;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record ExecutedByHashed(String hashed) {

    public ExecutedByHashed {
        Objects.requireNonNull(hashed);
        Validate.validState(ExecutedBy.Anonymous.DISCRIMINANT.equals(hashed)
                || hashed.startsWith(ExecutedBy.EndUser.DISCRIMINANT + ExecutedBy.EndUser.SEPARATOR)
                || hashed.startsWith(ExecutedBy.ServiceAccount.DISCRIMINANT + ExecutedBy.ServiceAccount.SEPARATOR)
                || ExecutedBy.NotAvailable.DISCRIMINANT.equals(hashed));
    }
}

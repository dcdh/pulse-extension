package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;
import java.util.Optional;

public class ExecutedByFactory {

    private final ExecutedByDecoder executedByDecoder;

    public ExecutedByFactory(final ExecutedByDecoder executedByDecoder) {
        this.executedByDecoder = Objects.requireNonNull(executedByDecoder);
    }

    public ExecutedBy from(final String value, final OwnedBy ownedBy) throws UnableToDecodeException {
        if (value == null || value.equals(ExecutedBy.NotAvailable.DISCRIMINANT)) {
            return ExecutedBy.NotAvailable.INSTANCE;
        } else if (value.equals(ExecutedBy.Anonymous.DISCRIMINANT)) {
            return ExecutedBy.Anonymous.INSTANCE;
        } else if (value.startsWith(ExecutedBy.EndUser.DISCRIMINANT + ExecutedBy.SEPARATOR)) {
            final String encodedEndUser = value.substring((ExecutedBy.EndUser.DISCRIMINANT + ExecutedBy.SEPARATOR).length());
            final Optional<String> decoded = executedByDecoder.decode(encodedEndUser, ownedBy);
            return decoded.map(decodedEndUser -> new ExecutedBy.EndUser(decodedEndUser, true))
                    .orElseGet(() -> new ExecutedBy.EndUser(encodedEndUser, false));
        } else if (value.startsWith(ExecutedBy.ServiceAccount.DISCRIMINANT + ExecutedBy.SEPARATOR)) {
            return new ExecutedBy.ServiceAccount(value.substring((ExecutedBy.ServiceAccount.DISCRIMINANT + ExecutedBy.SEPARATOR).length()));
        }
        throw new IllegalArgumentException("Invalid ExecutedBy value: " + value);
    }
}

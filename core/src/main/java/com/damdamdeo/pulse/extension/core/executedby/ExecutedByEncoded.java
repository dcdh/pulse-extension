package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
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

    public ExecutedBy to(final UsernameDecoder usernameDecoder, final OwnedBy ownedBy) throws UnableToDecodeException {
        Objects.requireNonNull(usernameDecoder);
        Objects.requireNonNull(ownedBy);
        if (encoded.equals(ExecutedBy.NotAvailable.DISCRIMINANT)) {
            return ExecutedBy.NotAvailable.INSTANCE;
        } else if (encoded.equals(ExecutedBy.Anonymous.DISCRIMINANT)) {
            return ExecutedBy.Anonymous.INSTANCE;
        } else if (encoded.startsWith(ExecutedBy.EndUser.DISCRIMINANT + ExecutedBy.SEPARATOR)) {
            final UsernameEncoded encodedEndUser = new UsernameEncoded(encoded.substring(
                    (ExecutedBy.EndUser.DISCRIMINANT + ExecutedBy.SEPARATOR).length()));
            return new ExecutedBy.EndUser(usernameDecoder.decode(encodedEndUser, ownedBy));
        } else if (encoded.startsWith(ExecutedBy.ServiceAccount.DISCRIMINANT + ExecutedBy.SEPARATOR)) {
            return new ExecutedBy.ServiceAccount(encoded.substring((ExecutedBy.ServiceAccount.DISCRIMINANT + ExecutedBy.SEPARATOR).length()));
        }
        throw new IllegalArgumentException("Invalid ExecutedBy value: " + encoded);
    }
}

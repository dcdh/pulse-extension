package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUser;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record ConnectionIdentifier(String id) implements Identifiable {

    public ConnectionIdentifier {
        Objects.requireNonNull(id);
        Validate.matchesPattern(id, "[a-zA-Z0-9]+");
    }

    public static ConnectionIdentifier from(final String id) {
        return new ConnectionIdentifier(id);
    }

    public static ConnectionIdentifier from(final Hash<ConnectedUser> connectedUserHashed) {
        return new ConnectionIdentifier(connectedUserHashed.value());
    }
}

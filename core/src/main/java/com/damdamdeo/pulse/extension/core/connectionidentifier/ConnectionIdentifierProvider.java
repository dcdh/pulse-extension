package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.connecteduser.*;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;

import java.util.Objects;

public class ConnectionIdentifierProvider {

    private final ConnectedUserProvider connectedUserProvider;
    private final Hasher hasher;

    public ConnectionIdentifierProvider(final ConnectedUserProvider connectedUserProvider,
                                        final Hasher hasher) {
        this.connectedUserProvider = Objects.requireNonNull(connectedUserProvider);
        this.hasher = Objects.requireNonNull(hasher);
    }

    public ConnectionIdentifier provide() throws ConnectionIdentifierProviderException {
        try {
            final ConnectedUser connectedUser = connectedUserProvider.provide();
            return ConnectionIdentifier.from(hasher.hash(connectedUser));
        } catch (final ConnectedIsAnonymousException | UsernameNotAMailException |
                       ConnectedUserNotAvailableException e) {
            throw new ConnectionIdentifierProviderException(e);
        }
    }
}

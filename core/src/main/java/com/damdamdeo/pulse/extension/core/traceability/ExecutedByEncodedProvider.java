package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.*;

import java.util.Objects;

public final class ExecutedByEncodedProvider {

    private final ExecutedByEncodedRepository executedByEncodedRepository;
    private final OwnedByProvider ownedByProvider;
    private final UsernameEncoder usernameEncoder;
    private final UsernameHasher usernameHasher;

    public ExecutedByEncodedProvider(final ExecutedByEncodedRepository executedByEncodedRepository,
                                     final OwnedByProvider ownedByProvider,
                                     final UsernameEncoder usernameEncoder,
                                     final UsernameHasher usernameHasher) {
        this.executedByEncodedRepository = Objects.requireNonNull(executedByEncodedRepository);
        this.ownedByProvider = Objects.requireNonNull(ownedByProvider);
        this.usernameEncoder = Objects.requireNonNull(usernameEncoder);
        this.usernameHasher = Objects.requireNonNull(usernameHasher);
    }

    public ExecutedByEncoded provide(final AnyAggregateId aggregateId, final ExecutedBy executedBy) throws ExecutedByEncoderException {
        Objects.requireNonNull(executedBy);
        Objects.requireNonNull(aggregateId);
        try {
            final ExecutedByHashed hashed = executedBy.hash(usernameHasher);
            final ExecutedByEncoded by = executedByEncodedRepository.findBy(hashed);
            if (by == null) {
                final ExecutedByEncoded encoded = executedBy.encode(usernameEncoder, ownedByProvider.provide(aggregateId));
                executedByEncodedRepository.store(hashed, encoded);
                return encoded;
            } else {
                return by;
            }
        } catch (final UnableToEncodeException | ExecutedByEncodedRepositoryException |
                       OwnedByProviderException exception) {
            throw new ExecutedByEncoderException(exception);
        }
    }
}

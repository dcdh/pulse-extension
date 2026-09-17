package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UsernameDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.damdamdeo.pulse.extension.core.traceability.Finder.ROLE_TRACEABILITY_READ;

public final class DefaultDetailedInvolvedFinder implements DetailedInvolvedFinder {

    private final EncodedDetailedInvolvedRepository encodedDetailedInvolvedRepository;
    private final OwnedByProvider ownedByProvider;
    private final UsernameDecoder usernameDecoder;
    private final ExecutionContextProvider executionContextProvider;

    public DefaultDetailedInvolvedFinder(final EncodedDetailedInvolvedRepository encodedDetailedInvolvedRepository,
                                         final OwnedByProvider ownedByProvider,
                                         final UsernameDecoder usernameDecoder,
                                         final ExecutionContextProvider executionContextProvider) {
        this.encodedDetailedInvolvedRepository = Objects.requireNonNull(encodedDetailedInvolvedRepository);
        this.ownedByProvider = Objects.requireNonNull(ownedByProvider);
        this.usernameDecoder = Objects.requireNonNull(usernameDecoder);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
    }

    @Override
    public Page<DetailedInvolved> findBy(final AggregateId aggregateId,
                                         final IncludeUncompounded includeUncompounded,
                                         final Pagination pagination) throws FinderException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(includeUncompounded);
        Objects.requireNonNull(pagination);
        return findBy(() -> encodedDetailedInvolvedRepository.findBy(aggregateId, includeUncompounded, pagination));
    }

    @Override
    public Page<DetailedInvolved> findBy(ExecutedByHashed executedByHashed, Pagination pagination) throws FinderException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(pagination);
        return findBy(() -> encodedDetailedInvolvedRepository.findBy(executedByHashed, pagination));
    }

    private Page<DetailedInvolved> findBy(final FinderSupplier<Page<EncodedDetailedInvolved>> supplier) throws FinderException {
        Objects.requireNonNull(supplier);
        try {
            if (!executionContextProvider.provide().hasRole(ROLE_TRACEABILITY_READ)) {
                throw new UnauthorizedException();
            }
            final Page<EncodedDetailedInvolved> involvedPage = supplier.get();
            final List<DetailedInvolved> list = new ArrayList<>(involvedPage.content().size());
            for (final EncodedDetailedInvolved encodedDetailedInvolved : involvedPage.content()) {
                final DetailedInvolved detailedInvolved = new DetailedInvolved(
                        encodedDetailedInvolved.traceId(),
                        encodedDetailedInvolved.aggregateId(),
                        new Actor(
                                encodedDetailedInvolved.encodedActor().executedByHashed(),
                                encodedDetailedInvolved.encodedActor().executedByEncoded()
                                        .to(usernameDecoder, ownedByProvider.provide(encodedDetailedInvolved.aggregateId())
                                        )
                        ),
                        encodedDetailedInvolved.from(),
                        encodedDetailedInvolved.executedAt()
                );
                list.add(detailedInvolved);
            }
            return new Page<>(list, involvedPage.pagination(), involvedPage.totalElements());
        } catch (final TraceRepositoryException | OwnedByProviderException | UnauthorizedException exception) {
            throw new FinderException(exception);
        }
    }
}

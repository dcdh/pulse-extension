package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UsernameDecoder;
import com.damdamdeo.pulse.extension.core.query.UnauthorizedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DefaultInvolvedFinder implements InvolvedFinder {

    private final EncodedInvolvedRepository encodedInvolvedRepository;
    private final OwnedByProvider ownedByProvider;
    private final UsernameDecoder usernameDecoder;
    private final ExecutionContextProvider executionContextProvider;

    public DefaultInvolvedFinder(final EncodedInvolvedRepository encodedInvolvedRepository,
                                 final OwnedByProvider ownedByProvider,
                                 final UsernameDecoder usernameDecoder,
                                 final ExecutionContextProvider executionContextProvider) {
        this.encodedInvolvedRepository = Objects.requireNonNull(encodedInvolvedRepository);
        this.ownedByProvider = Objects.requireNonNull(ownedByProvider);
        this.usernameDecoder = Objects.requireNonNull(usernameDecoder);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
    }

    @Override
    public Page<Involved> findBy(final AggregateId aggregateId, final IncludeUncompounded includeUncompounded,
                                 final Pagination pagination) throws FinderException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(includeUncompounded);
        Objects.requireNonNull(pagination);
        return findBy(() -> encodedInvolvedRepository.findBy(aggregateId, includeUncompounded, pagination));
    }

    @Override
    public Page<Involved> findBy(final ExecutedByHashed executedByHashed, final Pagination pagination) throws FinderException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(pagination);
        return findBy(() -> encodedInvolvedRepository.findBy(executedByHashed, pagination));
    }

    private Page<Involved> findBy(final FinderSupplier<Page<EncodedInvolved>> supplier) throws FinderException {
        Objects.requireNonNull(supplier);
        try {
            if (!executionContextProvider.provide().hasRole(ROLE_TRACEABILITY_READ)) {
                throw new UnauthorizedException();
            }
            final Page<EncodedInvolved> involvedPage = supplier.get();
            final List<Involved> list = new ArrayList<>(involvedPage.content().size());
            for (final EncodedInvolved encodedInvolved : involvedPage.content()) {
                final Involved involved = new Involved(
                        encodedInvolved.aggregateId(),
                        encodedInvolved.executedByHashed(),
                        encodedInvolved.executedByEncoded().to(usernameDecoder, ownedByProvider.provide(encodedInvolved.aggregateId())));
                list.add(involved);
            }
            return new Page<>(list, involvedPage.pagination(), involvedPage.totalElements());
        } catch (final TraceRepositoryException | OwnedByProviderException | UnauthorizedException exception) {
            throw new FinderException(exception);
        }
    }
}

package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UsernameDecoder;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DefaultInvolvedFinder implements InvolvedFinder {

    private final EncodedInvolvedRepository encodedInvolvedRepository;
    private final OwnedByProvider ownedByProvider;
    private final UsernameDecoder usernameDecoder;
    private final ExecutionContextProvider executionContextProvider;
    private final TraceAppender traceAppender;

    public DefaultInvolvedFinder(final EncodedInvolvedRepository encodedInvolvedRepository,
                                 final OwnedByProvider ownedByProvider,
                                 final UsernameDecoder usernameDecoder,
                                 final ExecutionContextProvider executionContextProvider,
                                 final TraceAppender traceAppender) {
        this.encodedInvolvedRepository = Objects.requireNonNull(encodedInvolvedRepository);
        this.ownedByProvider = Objects.requireNonNull(ownedByProvider);
        this.usernameDecoder = Objects.requireNonNull(usernameDecoder);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.traceAppender = Objects.requireNonNull(traceAppender);
    }

    @Override
    public Page<Involved> findBy(final Identifiable identifiable, final IncludeUncompounded includeUncompounded,
                                 final Pagination pagination) throws FinderException {
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(includeUncompounded);
        Objects.requireNonNull(pagination);
        return findBy(() -> encodedInvolvedRepository.findBy(identifiable, includeUncompounded, pagination));
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
            final List<Involved> listOfInvolved = new ArrayList<>(involvedPage.content().size());
            for (final EncodedInvolved encodedInvolved : involvedPage.content()) {
                final Involved involved = new Involved(
                        encodedInvolved.identifiable(),
                        encodedInvolved.linkTo(),
                        new Actor(
                                encodedInvolved.encodedActor().executedByHashed(),
                                encodedInvolved.encodedActor().executedByEncoded()
                                        .to(usernameDecoder, ownedByProvider.provide(encodedInvolved.identifiable()))),
                        encodedInvolved.commandNbOfTimes(),
                        encodedInvolved.commandUnauthorizedNbOfTimes(),
                        encodedInvolved.commandBusinessFailedNbOfTimes(),
                        encodedInvolved.queryNbOfTimes(),
                        encodedInvolved.queryUnauthorizedNbOfTimes(),
                        encodedInvolved.traceabilityNbOfTimes());
                listOfInvolved.add(involved);
            }
            traceAppender.append(listOfInvolved.stream().collect(Collectors.toMap(
                    Involved::identifiable, Involved::linkTo, (x1, x2) -> x1)));
            return new Page<>(listOfInvolved, involvedPage.pagination(), involvedPage.totalElements());
        } catch (final TraceRepositoryException | OwnedByProviderException | UnauthorizedException
                       | TraceAppenderException exception) {
            throw new FinderException(exception);
        }
    }
}

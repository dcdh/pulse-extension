package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.*;
import com.damdamdeo.pulse.extension.core.query.UnauthorizedException;

import java.util.Objects;

public final class TraceFinder {

    private static final String ROLE_TRACEABILITY_READ = "TRACEABILITY_READ";

    private final TraceRepository traceRepository;
    private final UsernameHasher usernameHasher;
    private final UsernameDecoder usernameDecoder;
    private final ExecutionContextProvider executionContextProvider;

    public TraceFinder(final TraceRepository traceRepository,
                       final UsernameHasher usernameHasher,
                       final UsernameDecoder usernameDecoder,
                       final ExecutionContextProvider executionContextProvider) {
        this.traceRepository = Objects.requireNonNull(traceRepository);
        this.usernameHasher = Objects.requireNonNull(usernameHasher);
        this.usernameDecoder = Objects.requireNonNull(usernameDecoder);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
    }

    public Page<SingleTraceDecoded> findBy(final AggregateId aggregateId, final Pagination pagination) throws FinderException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(pagination);
        return find(() -> traceRepository.findBy(aggregateId, pagination));
    }

    public Page<SingleTraceDecoded> findBy(final ExecutedBy executedBy, final Pagination pagination) throws FinderException {
        Objects.requireNonNull(executedBy);
        Objects.requireNonNull(pagination);
        return find(() -> traceRepository.findBy(TracedByHashed.from(executedBy.hash(usernameHasher)), pagination));
    }

    @FunctionalInterface
    private interface FinderSupplier<T> {

        T get() throws TraceRepositoryException;
    }

    private Page<SingleTraceDecoded> find(final FinderSupplier<Page<SingleTrace>> supplier) throws FinderException {
        Objects.requireNonNull(supplier);
        try {
            if (!executionContextProvider.provide().hasRole(ROLE_TRACEABILITY_READ)) {
                throw new UnauthorizedException();
            }
            final Page<SingleTrace> singleTracePage = supplier.get();
            return new Page<>(
                    singleTracePage.content().stream().map(singleTrace -> new SingleTraceDecoded(
                            singleTrace.traceId(),
                            singleTrace.tracedByHashed(),
                            singleTrace.executedByEncoded().to(usernameDecoder, singleTrace.ownedBy()),
                            singleTrace.executedAt(),
                            singleTrace.executionStatus()
                    )).toList(),
                    singleTracePage.pagination(),
                    singleTracePage.totalElements()
            );
        } catch (final TraceRepositoryException | UnauthorizedException exception) {
            throw new FinderException(exception);
        }
    }
}

package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UsernameHasher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DefaultTraceAppender implements TraceAppender {

    private final ExecutionContextProvider executionContextProvider;
    private final ExecutedAtProvider executedAtProvider;
    private final TraceIdGenerator traceIdGenerator;
    private final UsernameHasher usernameHasher;
    private final ExecutedByEncodedProvider executedByEncodedProvider;
    private final TraceRecorderRepository traceRecorderRepository;

    public DefaultTraceAppender(final ExecutionContextProvider executionContextProvider,
                                final ExecutedAtProvider executedAtProvider,
                                final TraceIdGenerator traceIdGenerator,
                                final UsernameHasher usernameHasher,
                                final ExecutedByEncodedProvider executedByEncodedProvider,
                                final TraceRecorderRepository traceRecorderRepository) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.executedAtProvider = Objects.requireNonNull(executedAtProvider);
        this.traceIdGenerator = Objects.requireNonNull(traceIdGenerator);
        this.usernameHasher = Objects.requireNonNull(usernameHasher);
        this.executedByEncodedProvider = Objects.requireNonNull(executedByEncodedProvider);
        this.traceRecorderRepository = Objects.requireNonNull(traceRecorderRepository);
    }

    @Override
    public void append(final Traceable traceable, final From from) throws TraceAppenderException {
        Objects.requireNonNull(traceable);
        Objects.requireNonNull(from);
        try {
            if (traceable.aggregateIds().isEmpty()) {
                return;
            }
            final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
            final List<EncodedTraceAggregateId> encodedTraceAggregateIds = new ArrayList<>(traceable.aggregateIds().size());
            for (final AggregateId aggregateId : traceable.aggregateIds()) {
                EncodedTraceAggregateId encodedTraceAggregateId = new EncodedTraceAggregateId(aggregateId,
                        executedBy.hash(usernameHasher),
                        executedByEncodedProvider.provide(aggregateId, executedBy));
                encodedTraceAggregateIds.add(encodedTraceAggregateId);
            }
            traceRecorderRepository.store(new TraceRecorder(traceIdGenerator.generate(), executedAtProvider.now(),
                    from, encodedTraceAggregateIds));
        } catch (final TraceIdGeneratorException | ExecutedByEncoderException | TraceRepositoryException exception) {
            throw new TraceAppenderException(exception);
        }
    }
}

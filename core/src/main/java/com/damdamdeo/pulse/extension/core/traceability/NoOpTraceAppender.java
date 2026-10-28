package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.Projection;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class NoOpTraceAppender implements TraceAppender {

    @Override
    public <A extends AggregateId> void append(final Command<A> command, final List<VersionizedEvent<A>> versionizedEvents,
                                               final ExecutionStatus executionStatus) throws TraceAppenderException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(versionizedEvents);
        Objects.requireNonNull(executionStatus);
        // no-op
    }

    @Override
    public <A extends AggregateId, P extends Projection<A>> void append(final Input input, final List<P> projections,
                                                                        final ExecutionStatus executionStatus,
                                                                        final TraceabilityDetails traceabilityDetails)
            throws TraceAppenderException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(projections);
        Objects.requireNonNull(executionStatus);
        Objects.requireNonNull(traceabilityDetails);
        // no-op
    }

    @Override
    public void append(final Set<AggregateId> aggregateIds) throws TraceAppenderException {
        Objects.requireNonNull(aggregateIds);
        // no-op
    }
}

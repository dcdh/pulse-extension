package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.Projection;

import java.util.List;
import java.util.Set;

public interface TraceAppender {

    <A extends AggregateId> void append(Command<A> command, List<VersionizedEvent<A>> versionizedEvents,
                                        ExecutionStatus executionStatus) throws TraceAppenderException;

    <A extends AggregateId, P extends Projection<A>> void append(Input input, List<P> projections, ExecutionStatus executionStatus,
                                                                 TraceabilityDetails traceabilityDetails)
            throws TraceAppenderException;

    void append(Set<AggregateId> aggregateIds) throws TraceAppenderException;
}

package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.Projection;

import java.util.List;
import java.util.Map;

public interface TraceAppender {

    <A extends AggregateId> void append(Command<A> command, List<VersionizedEvent<A>> versionizedEvents,
                                        ExecutionStatus executionStatus) throws TraceAppenderException;

    <A extends AggregateId, P extends Projection<A>> void append(Input input, List<P> projections, ExecutionStatus executionStatus,
                                                                 TraceabilityDetails traceabilityDetails)
            throws TraceAppenderException;

    void append(Map<Identifiable, AnyAggregateId> identifiables) throws TraceAppenderException;
}

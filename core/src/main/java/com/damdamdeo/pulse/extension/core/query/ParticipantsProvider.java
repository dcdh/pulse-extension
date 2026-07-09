package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Map;
import java.util.Set;

public interface ParticipantsProvider {

    Set<ExecutedBy> findParticipants(Set<AggregateId> aggregatesId);

    Map<ExecutedBy, Set<ExecutedBy>> findParticipantRelations(Set<AggregateId> aggregateIds);
}

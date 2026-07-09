package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.ParticipantsProvider;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
@DefaultBean
public final class JdbcPostgresParticipantsProvider implements ParticipantsProvider {

    @Inject
    DataSource dataSource;

    @Override
    public Set<ExecutedBy> findParticipants(final Set<AggregateId> aggregatesId) {
        Objects.requireNonNull(aggregatesId);
        if (aggregatesId.isEmpty()) {
            return Set.of();
        }
        return;
    }

    @Override
    public Map<ExecutedBy, Set<ExecutedBy>> findParticipantRelations(final Set<AggregateId> aggregateIds) {
        Objects.requireNonNull(aggregateIds);
        if (aggregateIds.isEmpty()) {
            return Map.of();
        }
        return
    }

    faire le
    necessaire ...
    TODO passer
    par un
    trigger pour
    stocker les
    executedby !
}

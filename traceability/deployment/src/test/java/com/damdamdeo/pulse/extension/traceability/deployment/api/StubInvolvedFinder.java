package com.damdamdeo.pulse.extension.traceability.deployment.api;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connectionidentifier.AnyIdentifiable;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Priority(1)
@Alternative
public class StubInvolvedFinder implements InvolvedFinder {

    @Override
    public AggregateIdInvolved findBy(final Identifiable identifiable, final Pagination pagination) throws FinderException {
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(pagination);
        if (identifiable.equals(new AnyIdentifiable("BOOM"))) {
            throw new FinderException(new RuntimeException("BOOM"));
        }
        final DetailedInvolved aliceDetailedInvolved = detail(new Actor(
                new ExecutedByHashed("EU:alice-hashed"), new ExecutedBy.EndUser(new Username("alice@mail.com"))));
        final DetailedInvolved bobDetailedInvolved = detail(new Actor(
                new ExecutedByHashed("EU:bob-hashed"), new ExecutedBy.EndUser(new Username("bob@mail.com"))));
        return new AggregateIdInvolved(identifiable, AnyAggregateId.from(TodoId.USER_1_TODO_1),
                new Page<>(List.of(aliceDetailedInvolved, bobDetailedInvolved), pagination, 2L),
                List.of(aliceDetailedInvolved.nbOfExecutions(), bobDetailedInvolved.nbOfExecutions()));
    }

    @Override
    public ExecutedByInvolved findBy(final ExecutedByHashed executedByHashed, final Pagination pagination) throws FinderException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(pagination);
        final Actor actor = new Actor(executedByHashed, new ExecutedBy.EndUser(new Username("alice@mail.com")));
        return new ExecutedByInvolved(new Page<>(List.of(detail(actor)), pagination, 1L));
    }

    private static DetailedInvolved detail(final Actor actor) {
        final NbOfExecutions nbOfExecutions = new NbOfExecutions(actor.executedBy(), CommandNbOfTimes.ONE,
                new CommandUnauthorizedNbOfTimes(0), new CommandBusinessFailedNbOfTimes(0),
                QueryNbOfTimes.NEVER, new QueryUnauthorizedNbOfTimes(0), TraceabilityNbOfTimes.NEVER);
        return new DetailedInvolved(new TraceId(1L), new CorrelationId(1L), actor, new EventType("TodoCreated"),
                new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("from"),
                new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), nbOfExecutions);
    }
}

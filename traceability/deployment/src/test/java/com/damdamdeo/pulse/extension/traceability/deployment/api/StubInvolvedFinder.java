package com.damdamdeo.pulse.extension.traceability.deployment.api;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.*;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Priority(1)
@Alternative
public class StubInvolvedFinder implements InvolvedFinder {

    @Override
    public Page<Involved> findBy(final AggregateId aggregateId, final IncludeUncompounded includeUncompounded, final Pagination pagination) throws FinderException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(includeUncompounded);
        Validate.validState(includeUncompounded.included() == true);// force setting the query parameter
        Objects.requireNonNull(pagination);
        if (aggregateId.equals(new AnyAggregateId("BOOM"))) {
            throw new FinderException(new RuntimeException("BOOM"));
        }
        return new Page<>(
                List.of(
                        new Involved(TodoId.USER_1_TODO_1, new Actor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedBy.EndUser(new Username("alice@mail.com"))),
                                new CommandNbOfTimes(1), new QueryNbOfTimes(0)),
                        new Involved(TodoId.USER_1_TODO_1, new Actor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedBy.EndUser(new Username("bob@mail.com"))),
                                new CommandNbOfTimes(1), new QueryNbOfTimes(0))),
                new Pagination(0, 10), 2L);
    }

    @Override
    public Page<Involved> findBy(final ExecutedByHashed executedByHashed, final Pagination pagination) throws FinderException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(pagination);
        return new Page<>(
                List.of(
                        new Involved(AnyAggregateId.from("U000001-T000001"), new Actor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedBy.EndUser(new Username("alice@mail.com"))),
                                new CommandNbOfTimes(1), new QueryNbOfTimes(0))),
                new Pagination(0, 10), 1L);
    }
}

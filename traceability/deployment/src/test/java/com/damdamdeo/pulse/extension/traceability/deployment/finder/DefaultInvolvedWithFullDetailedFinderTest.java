package com.damdamdeo.pulse.extension.traceability.deployment.finder;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;
import com.damdamdeo.pulse.extension.traceability.deployment.AbstractTraceTest;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class DefaultInvolvedWithFullDetailedFinderTest extends AbstractTraceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubUsernameEncoder.class,
                    StubOwnedByProvider.class, StubExecutionContextProvider.class, StubUsernameDecoder.class))
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Inject
    DefaultInvolvedFinder defaultDetailedInvolvedFinder;

    @Test
    @RunOnVertxContext
    void shouldFindByIdentifiable() throws FinderException {
        // Given

        // When
        final AggregateIdInvolved by = defaultDetailedInvolvedFinder.findBy(TodoId.USER_1_TODO_1, new Pagination(0, 10));

        // Then
        final Actor alice = new Actor(new ExecutedByHashed("EU:alice-hashed"),
                new ExecutedBy.EndUser(new Username("alice@mail.com")));
        final Actor bob = new Actor(new ExecutedByHashed("EU:bob-hashed"),
                new ExecutedBy.EndUser(new Username("bob@mail.com")));
        final NbOfExecutions aliceCounts = new NbOfExecutions(alice.executedBy(), new CommandNbOfTimes(3),
                CommandUnauthorizedNbOfTimes.NEVER, CommandBusinessFailedNbOfTimes.NEVER,
                QueryNbOfTimes.NEVER, QueryUnauthorizedNbOfTimes.NEVER, TraceabilityNbOfTimes.NEVER);
        final NbOfExecutions bobCounts = new NbOfExecutions(bob.executedBy(), new CommandNbOfTimes(0),
                new CommandUnauthorizedNbOfTimes(1), new CommandBusinessFailedNbOfTimes(1),
                QueryNbOfTimes.NEVER, QueryUnauthorizedNbOfTimes.ONE, TraceabilityNbOfTimes.NEVER);

        assertAll(
                () -> assertThat(by.identifiable()).isEqualTo(TodoId.USER_1_TODO_1),
                () -> assertThat(by.linkTo()).isEqualTo(AnyAggregateId.from(TodoId.USER_1_TODO_1)),
                () -> assertThat(by.nbOfExecutions()).containsExactlyInAnyOrder(aliceCounts, bobCounts),
                () -> assertThat(by.page()).isEqualTo(new Page<>(List.of(
                        new DetailedInvolved(new TraceId(1L), new CorrelationId(1L), alice,
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS,
                                new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts),
                        new DetailedInvolved(new TraceId(1L), new CorrelationId(1L), alice,
                                new EventType("TodoUpdated"), new AggregateVersion(1), Source.COMMAND, ExecutionStatus.SUCCESS,
                                new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts),
                        new DetailedInvolved(new TraceId(1L), new CorrelationId(1L), alice,
                                new EventType("TodoUpdated"), new AggregateVersion(2), Source.COMMAND, ExecutionStatus.SUCCESS,
                                new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts),
                        new DetailedInvolved(new TraceId(2L), new CorrelationId(2L), bob,
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_UNAUTHORIZED,
                                new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T13:00:00Z")), bobCounts),
                        new DetailedInvolved(new TraceId(3L), new CorrelationId(3L), bob,
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_BUSINESS,
                                new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T14:00:00Z")), bobCounts),
                        new DetailedInvolved(new TraceId(5L), new CorrelationId(5L), bob,
                                null, null, Source.QUERY, ExecutionStatus.FAILED_UNAUTHORIZED,
                                new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T16:00:00Z")), bobCounts)),
                        new Pagination(0, 10), 6L))
        );
    }

    @Test
    @RunOnVertxContext
    void shouldFindByExecutedByHashed() throws FinderException {
        // Given

        // When
        final ExecutedByInvolved by = defaultDetailedInvolvedFinder.findBy(new ExecutedByHashed("EU:alice-hashed"), new Pagination(0, 10));

        // Then
        final Actor alice = new Actor(new ExecutedByHashed("EU:alice-hashed"),
                new ExecutedBy.EndUser(new Username("alice@mail.com")));
        final NbOfExecutions counts = new NbOfExecutions(alice.executedBy(), new CommandNbOfTimes(3),
                CommandUnauthorizedNbOfTimes.NEVER, CommandBusinessFailedNbOfTimes.NEVER,
                QueryNbOfTimes.NEVER, QueryUnauthorizedNbOfTimes.NEVER, TraceabilityNbOfTimes.NEVER);

        assertThat(by).isEqualTo(new ExecutedByInvolved(new Page<>(List.of(
                new DetailedInvolved(new TraceId(1L), new CorrelationId(1L), alice,
                        new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS,
                        new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), counts),
                new DetailedInvolved(new TraceId(1L), new CorrelationId(1L), alice,
                        new EventType("TodoUpdated"), new AggregateVersion(1), Source.COMMAND, ExecutionStatus.SUCCESS,
                        new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), counts),
                new DetailedInvolved(new TraceId(1L), new CorrelationId(1L), alice,
                        new EventType("TodoUpdated"), new AggregateVersion(2), Source.COMMAND, ExecutionStatus.SUCCESS,
                        new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), counts)),
                new Pagination(0, 10), 3L)));
    }
}

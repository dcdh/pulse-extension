package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.connectionidentifier.AnyIdentifiable;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresInvolvedFinderRepository;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbcPostgresInvolvedWithFullDetailsFinderRepositoryTest extends AbstractTraceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresInvolvedFinderRepository jdbcPostgresInvolvedFinderRepository;

    @Test
    void shouldFindByIdentifiable() throws TraceRepositoryException {
        // Given
        final List<EncodedAggregateIdInvolved> executions = new ArrayList<>(2);

        // When
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(TodoId.USER_1_TODO_1, new Pagination(0, 1)));
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(TodoId.USER_1_TODO_1, new Pagination(1, 1)));
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(TodoId.USER_1_TODO_1, new Pagination(0, -1)));

        final EncodedActor alice = new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded"));
        final EncodedActor bob = new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded"));
        final EncodedNbOfExecutions aliceCounts = new EncodedNbOfExecutions(alice, new CommandNbOfTimes(3),
                CommandUnauthorizedNbOfTimes.NEVER, CommandBusinessFailedNbOfTimes.NEVER,
                QueryNbOfTimes.NEVER, QueryUnauthorizedNbOfTimes.NEVER, TraceabilityNbOfTimes.NEVER);
        final EncodedNbOfExecutions bobTodoOneCounts = new EncodedNbOfExecutions(bob, new CommandNbOfTimes(0),
                new CommandUnauthorizedNbOfTimes(1), new CommandBusinessFailedNbOfTimes(1),
                QueryNbOfTimes.NEVER, QueryUnauthorizedNbOfTimes.ONE, TraceabilityNbOfTimes.NEVER);
        assertThat(executions).containsExactly(
                new EncodedAggregateIdInvolved(TodoId.USER_1_TODO_1, new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyIdentifiable.from(TodoId.USER_1_TODO_1), AnyAggregateId.from(TodoId.USER_1_TODO_1), alice,
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts)),
                        new Pagination(0, 1), 6L), Set.of(aliceCounts)),
                new EncodedAggregateIdInvolved(TodoId.USER_1_TODO_1, new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyIdentifiable.from(TodoId.USER_1_TODO_1), AnyAggregateId.from(TodoId.USER_1_TODO_1), alice,
                                new EventType("TodoUpdated"), new AggregateVersion(1), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts)),
                        new Pagination(1, 1), 6L), Set.of(aliceCounts)),
                new EncodedAggregateIdInvolved(TodoId.USER_1_TODO_1, new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyIdentifiable.from(TodoId.USER_1_TODO_1), AnyAggregateId.from(TodoId.USER_1_TODO_1), alice,
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts),
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyIdentifiable.from(TodoId.USER_1_TODO_1), AnyAggregateId.from(TodoId.USER_1_TODO_1), alice,
                                new EventType("TodoUpdated"), new AggregateVersion(1), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts),
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyIdentifiable.from(TodoId.USER_1_TODO_1), AnyAggregateId.from(TodoId.USER_1_TODO_1), alice,
                                new EventType("TodoUpdated"), new AggregateVersion(2), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts),
                        new EncodedDetailedInvolved(new TraceId(2L), new CorrelationId(2L), AnyIdentifiable.from(TodoId.USER_1_TODO_1), AnyAggregateId.from(TodoId.USER_1_TODO_1), bob,
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T13:00:00Z")), bobTodoOneCounts),
                        new EncodedDetailedInvolved(new TraceId(3L), new CorrelationId(3L), AnyIdentifiable.from(TodoId.USER_1_TODO_1), AnyAggregateId.from(TodoId.USER_1_TODO_1), bob,
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_BUSINESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T14:00:00Z")), bobTodoOneCounts),
                        new EncodedDetailedInvolved(new TraceId(5L), new CorrelationId(5L), AnyIdentifiable.from(TodoId.USER_1_TODO_1), AnyAggregateId.from(TodoId.USER_1_TODO_1), bob,
                                null, null, Source.QUERY, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T16:00:00Z")), bobTodoOneCounts)),
                        new Pagination(0, -1), 6L), Set.of(aliceCounts, bobTodoOneCounts))
        );
    }

    @Test
    void shouldFindByExecutedByHashed() throws TraceRepositoryException {
        // Given
        final List<EncodedExecutedByInvolved> executions = new ArrayList<>(2);

        // When
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(new ExecutedByHashed("EU:bob-hashed"), new Pagination(0, 1)));
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(new ExecutedByHashed("EU:bob-hashed"), new Pagination(1, 1)));
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(new ExecutedByHashed("EU:bob-hashed"), new Pagination(0, -1)));
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(new ExecutedByHashed("EU:alice-hashed"), new Pagination(0, -1)));

        // Then
        final EncodedActor alice = new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded"));
        final EncodedActor bob = new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded"));
        final EncodedNbOfExecutions aliceCounts = new EncodedNbOfExecutions(alice, new CommandNbOfTimes(3),
                CommandUnauthorizedNbOfTimes.NEVER, CommandBusinessFailedNbOfTimes.NEVER,
                QueryNbOfTimes.NEVER, QueryUnauthorizedNbOfTimes.NEVER, TraceabilityNbOfTimes.NEVER);
        final EncodedNbOfExecutions bobTodoOneCounts = new EncodedNbOfExecutions(bob, new CommandNbOfTimes(0),
                new CommandUnauthorizedNbOfTimes(1), new CommandBusinessFailedNbOfTimes(1),
                QueryNbOfTimes.NEVER, QueryUnauthorizedNbOfTimes.ONE, TraceabilityNbOfTimes.NEVER);
        final EncodedNbOfExecutions bobTodoTwoCounts = new EncodedNbOfExecutions(bob, CommandNbOfTimes.ONE,
                CommandUnauthorizedNbOfTimes.NEVER, CommandBusinessFailedNbOfTimes.NEVER,
                QueryNbOfTimes.ONE, QueryUnauthorizedNbOfTimes.NEVER, new TraceabilityNbOfTimes(2));
        final EncodedNbOfExecutions bobChecklistCounts = new EncodedNbOfExecutions(bob, new CommandNbOfTimes(0),
                CommandUnauthorizedNbOfTimes.NEVER, CommandBusinessFailedNbOfTimes.NEVER,
                QueryNbOfTimes.NEVER, QueryUnauthorizedNbOfTimes.ONE, TraceabilityNbOfTimes.NEVER);
        assertThat(executions).containsExactly(
                new EncodedExecutedByInvolved(new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(2L), new CorrelationId(2L),
                                new AnyIdentifiable("U000001-T000001"), AnyAggregateId.from("U000001-T000001"),
                                bob,
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T13:00:00Z")), bobTodoOneCounts)),
                        new Pagination(0, 1), 8L)),
                new EncodedExecutedByInvolved(new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(3L), new CorrelationId(3L),
                                new AnyIdentifiable("U000001-T000001"), AnyAggregateId.from("U000001-T000001"),
                                bob,
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_BUSINESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T14:00:00Z")), bobTodoOneCounts)),
                        new Pagination(1, 1), 8L)),
                new EncodedExecutedByInvolved(new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(2L), new CorrelationId(2L),
                                new AnyIdentifiable("U000001-T000001"), AnyAggregateId.from("U000001-T000001"),
                                bob,
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T13:00:00Z")), bobTodoOneCounts),
                        new EncodedDetailedInvolved(new TraceId(3L), new CorrelationId(3L),
                                new AnyIdentifiable("U000001-T000001"), AnyAggregateId.from("U000001-T000001"),
                                bob,
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_BUSINESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T14:00:00Z")), bobTodoOneCounts),
                        new EncodedDetailedInvolved(new TraceId(4L), new CorrelationId(4L),
                                new AnyIdentifiable("U000001-T000002"), AnyAggregateId.from("U000001-T000002"),
                                bob,
                                null, null, Source.QUERY, ExecutionStatus.SUCCESS, new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T15:00:00Z")), bobTodoTwoCounts),
                        new EncodedDetailedInvolved(new TraceId(5L), new CorrelationId(5L),
                                new AnyIdentifiable("U000001-T000001"), AnyAggregateId.from("U000001-T000001"),
                                bob,
                                null, null, Source.QUERY, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T16:00:00Z")), bobTodoOneCounts),
                        new EncodedDetailedInvolved(new TraceId(6L), new CorrelationId(6L),
                                new AnyIdentifiable("U000001-T000001-CL000001"), AnyAggregateId.from("U000001-T000001-CL000001"),
                                bob,
                                null, null, Source.QUERY, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T17:00:00Z")), bobChecklistCounts),
                        new EncodedDetailedInvolved(new TraceId(7L), new CorrelationId(7L),
                                new AnyIdentifiable("U000001-T000002"), AnyAggregateId.from("U000001-T000002"),
                                bob,
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T18:00:00Z")), bobTodoTwoCounts),
                        new EncodedDetailedInvolved(new TraceId(8L), new CorrelationId(8L),
                                new AnyIdentifiable("U000001-T000002"), AnyAggregateId.from("U000001-T000002"),
                                bob,
                                null, null, Source.TRACEABILITY, ExecutionStatus.SUCCESS, new From("TRACEABILITY"), new ExecutedAt(Instant.parse("2026-09-06T19:00:00Z")), bobTodoTwoCounts),
                        new EncodedDetailedInvolved(new TraceId(9L), new CorrelationId(9L),
                                new AnyIdentifiable("U000001-T000002"), AnyAggregateId.from("U000001-T000002"),
                                bob,
                                null, null, Source.TRACEABILITY, ExecutionStatus.SUCCESS, new From("TRACEABILITY"), new ExecutedAt(Instant.parse("2026-09-06T20:00:00Z")), bobTodoTwoCounts)),
                        new Pagination(0, -1), 8L)),
                new EncodedExecutedByInvolved(new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L),
                                new AnyIdentifiable("U000001-T000001"), AnyAggregateId.from("U000001-T000001"),
                                alice,
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts),
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L),
                                new AnyIdentifiable("U000001-T000001"), AnyAggregateId.from("U000001-T000001"),
                                alice,
                                new EventType("TodoUpdated"), new AggregateVersion(1), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts),
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L),
                                new AnyIdentifiable("U000001-T000001"), AnyAggregateId.from("U000001-T000001"),
                                alice,
                                new EventType("TodoUpdated"), new AggregateVersion(2), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")), aliceCounts)
                ), new Pagination(0, -1), 3L))
        );
    }
}

package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresEncodedDetailedInvolvedRepository;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JdbcPostgresEncodedDetailedInvolvedRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresEncodedDetailedInvolvedRepository jdbcPostgresEncodedDetailedInvolvedRepository;

    @Inject
    JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository jdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository;

    @BeforeAll
    void prepare() throws TraceRepositoryException {
        for (final TraceRecorder traceRecorder : List.of(
                new TraceRecorder(
                        new TraceId(1L),
                        new CorrelationId(1L),
                        new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")),
                        Source.COMMAND,
                        ExecutionStatus.SUCCESS,
                        new From("fromCommand"),
                        List.of(
                                EncodedTraceAggregateId.fromCommand(TodoId.USER_1_TODO_1, new ExecutedByHashed("EU:alice-hashed"),
                                        new ExecutedByEncoded("EU:aliceEncoded"),
                                        new EventType("TodoCreated"), new AggregateVersion(0)),
                                EncodedTraceAggregateId.fromCommand(TodoId.USER_1_TODO_1, new ExecutedByHashed("EU:alice-hashed"),
                                        new ExecutedByEncoded("EU:aliceEncoded"),
                                        new EventType("TodoUpdated"), new AggregateVersion(1)),
                                EncodedTraceAggregateId.fromCommand(TodoId.USER_1_TODO_1, new ExecutedByHashed("EU:alice-hashed"),
                                        new ExecutedByEncoded("EU:aliceEncoded"),
                                        new EventType("TodoUpdated"), new AggregateVersion(2)))),
                new TraceRecorder(
                        new TraceId(2L),
                        new CorrelationId(2L),
                        new ExecutedAt(Instant.parse("2026-09-06T13:00:00Z")),
                        Source.COMMAND,
                        ExecutionStatus.FAILED_UNAUTHORIZED,
                        new From("fromCommand"),
                        List.of(
                                EncodedTraceAggregateId.fromCommand(TodoId.USER_1_TODO_1, new ExecutedByHashed("EU:bob-hashed"),
                                        new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        new TraceId(3L),
                        new CorrelationId(3L),
                        new ExecutedAt(Instant.parse("2026-09-06T14:00:00Z")),
                        Source.COMMAND,
                        ExecutionStatus.FAILED_BUSINESS,
                        new From("fromCommand"),
                        List.of(
                                EncodedTraceAggregateId.fromCommand(TodoId.USER_1_TODO_1, new ExecutedByHashed("EU:bob-hashed"),
                                        new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        new TraceId(4L),
                        new CorrelationId(4L),
                        new ExecutedAt(Instant.parse("2026-09-06T15:00:00Z")),
                        Source.QUERY,
                        ExecutionStatus.SUCCESS,
                        new From("fromQuery"),
                        List.of(
                                EncodedTraceAggregateId.fromQuery(TodoId.USER_1_TODO_2, new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        new TraceId(5L),
                        new CorrelationId(5L),
                        new ExecutedAt(Instant.parse("2026-09-06T16:00:00Z")),
                        Source.QUERY,
                        ExecutionStatus.FAILED_UNAUTHORIZED,
                        new From("fromQuery"),
                        List.of(
                                EncodedTraceAggregateId.fromQuery(TodoId.USER_1_TODO_1, new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        new TraceId(6L),
                        new CorrelationId(6L),
                        new ExecutedAt(Instant.parse("2026-09-06T17:00:00Z")),
                        Source.QUERY,
                        ExecutionStatus.FAILED_UNAUTHORIZED,
                        new From("fromQuery"),
                        List.of(
                                EncodedTraceAggregateId.fromQuery(TodoChecklistId.USER_1_TODO_1_1, new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        new TraceId(7L),
                        new CorrelationId(7L),
                        new ExecutedAt(Instant.parse("2026-09-06T18:00:00Z")),
                        Source.COMMAND,
                        ExecutionStatus.SUCCESS,
                        new From("fromCommand"),
                        List.of(
                                EncodedTraceAggregateId.fromCommand(TodoId.USER_1_TODO_2, new ExecutedByHashed("EU:bob-hashed"),
                                        new ExecutedByEncoded("EU:bobEncoded"),
                                        new EventType("TodoCreated"), new AggregateVersion(0))))
        )) {
            jdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository.store(traceRecorder);
        }
    }

    @Test
    void shouldFindByAggregateId() throws TraceRepositoryException {
        // Given
        final List<Page<EncodedDetailedInvolved>> executions = new ArrayList<>(2);

        // When
        executions.add(jdbcPostgresEncodedDetailedInvolvedRepository.findBy(TodoId.USER_1_TODO_1,
                new IncludeUncompounded(false), new Pagination(0, 1)));
        executions.add(jdbcPostgresEncodedDetailedInvolvedRepository.findBy(TodoId.USER_1_TODO_1,
                new IncludeUncompounded(false), new Pagination(1, 1)));
        executions.add(jdbcPostgresEncodedDetailedInvolvedRepository.findBy(TodoId.USER_1_TODO_1,
                new IncludeUncompounded(false), new Pagination(0, -1)));

        assertThat(executions).containsExactly(
                new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")))),
                        new Pagination(0, 1), 6L),
                new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoUpdated"), new AggregateVersion(1), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")))),
                        new Pagination(1, 1), 6L),
                new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoUpdated"), new AggregateVersion(1), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoUpdated"), new AggregateVersion(2), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(2L), new CorrelationId(2L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T13:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(3L), new CorrelationId(3L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_BUSINESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T14:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(5L), new CorrelationId(5L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.QUERY, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T16:00:00Z")))),
                        new Pagination(0, -1), 6L)
        );
    }

    @Test
    void shouldFindByAggregateIdWithUncompounded() throws TraceRepositoryException {
        // Given
        final List<Page<EncodedDetailedInvolved>> executions = new ArrayList<>(2);

        // When
        executions.add(jdbcPostgresEncodedDetailedInvolvedRepository.findBy(TodoId.USER_1_TODO_1,
                new IncludeUncompounded(true), new Pagination(0, 1)));
        executions.add(jdbcPostgresEncodedDetailedInvolvedRepository.findBy(TodoId.USER_1_TODO_1,
                new IncludeUncompounded(true), new Pagination(1, 1)));
        executions.add(jdbcPostgresEncodedDetailedInvolvedRepository.findBy(TodoId.USER_1_TODO_1,
                new IncludeUncompounded(true), new Pagination(0, -1)));

        // Then
        assertThat(executions).containsExactly(
                new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")))),
                        new Pagination(0, 1), 7L),
                new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoUpdated"), new AggregateVersion(1), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")))),
                        new Pagination(1, 1), 7L),
                new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoUpdated"), new AggregateVersion(1), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoUpdated"), new AggregateVersion(2), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(2L), new CorrelationId(2L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T13:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(3L), new CorrelationId(3L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_BUSINESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T14:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(5L), new CorrelationId(5L), AnyAggregateId.from(TodoId.USER_1_TODO_1), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.QUERY, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T16:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(6L), new CorrelationId(6L), AnyAggregateId.from(TodoChecklistId.USER_1_TODO_1_1), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.QUERY, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T17:00:00Z")))),
                        new Pagination(0, -1), 7L)
        );
    }

    @Test
    void shouldFindByExecutedByHashed() throws TraceRepositoryException {
        // Given
        final List<Page<EncodedDetailedInvolved>> executions = new ArrayList<>(2);

        // When
        executions.add(jdbcPostgresEncodedDetailedInvolvedRepository.findBy(new ExecutedByHashed("EU:bob-hashed"), new Pagination(0, 1)));
        executions.add(jdbcPostgresEncodedDetailedInvolvedRepository.findBy(new ExecutedByHashed("EU:bob-hashed"), new Pagination(1, 1)));
        executions.add(jdbcPostgresEncodedDetailedInvolvedRepository.findBy(new ExecutedByHashed("EU:bob-hashed"), new Pagination(0, -1)));
        executions.add(jdbcPostgresEncodedDetailedInvolvedRepository.findBy(new ExecutedByHashed("EU:alice-hashed"), new Pagination(0, -1)));

        // Then
        assertThat(executions).containsExactly(
                new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(2L), new CorrelationId(2L), AnyAggregateId.from("U000001-T000001"), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T13:00:00Z")))),
                        new Pagination(0, 1), 6L),
                new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(3L), new CorrelationId(3L), AnyAggregateId.from("U000001-T000001"), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_BUSINESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T14:00:00Z")))),
                        new Pagination(1, 1), 6L),
                new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(2L), new CorrelationId(2L), AnyAggregateId.from("U000001-T000001"), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T13:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(3L), new CorrelationId(3L), AnyAggregateId.from("U000001-T000001"), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.COMMAND, ExecutionStatus.FAILED_BUSINESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T14:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(4L), new CorrelationId(4L), AnyAggregateId.from("U000001-T000002"), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.QUERY, ExecutionStatus.SUCCESS, new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T15:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(5L), new CorrelationId(5L), AnyAggregateId.from("U000001-T000001"), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.QUERY, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T16:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(6L), new CorrelationId(6L), AnyAggregateId.from("U000001-T000001-CL000001"), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                null, null, Source.QUERY, ExecutionStatus.FAILED_UNAUTHORIZED, new From("fromQuery"), new ExecutedAt(Instant.parse("2026-09-06T17:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(7L), new CorrelationId(7L), AnyAggregateId.from("U000001-T000002"), new EncodedActor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")),
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T18:00:00Z")))),
                        new Pagination(0, -1), 6L),
                new Page<>(List.of(
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from("U000001-T000001"), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoCreated"), new AggregateVersion(0), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from("U000001-T000001"), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoUpdated"), new AggregateVersion(1), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"))),
                        new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(1L), AnyAggregateId.from("U000001-T000001"), new EncodedActor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EventType("TodoUpdated"), new AggregateVersion(2), Source.COMMAND, ExecutionStatus.SUCCESS, new From("fromCommand"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")))
                ), new Pagination(0, -1), 3L)
        );
    }
}

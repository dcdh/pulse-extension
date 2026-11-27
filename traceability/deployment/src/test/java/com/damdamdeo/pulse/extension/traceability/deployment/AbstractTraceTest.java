package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.TodoChecklistProjection;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresTraceIdGenerator;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresTraceRecorderRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractTraceTest {

    @Inject
    JdbcPostgresTraceRecorderRepository jdbcPostgresTraceRecorderRepository;

    @Inject
    JdbcPostgresTraceIdGenerator jdbcPostgresTraceIdGenerator;

    @BeforeAll
    void prepare() throws TraceRepositoryException, TraceIdGeneratorException {
        for (final TraceRecorder traceRecorder : List.of(
                new TraceRecorder(
                        jdbcPostgresTraceIdGenerator.generate(),
                        new CorrelationId(1L),
                        new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")),
                        Source.COMMAND,
                        ExecutionStatus.SUCCESS,
                        new From("fromCommand"),
                        List.of(
                                EncodedTraceIdentifiable.from(new MarkTodoAsDone(TodoId.USER_1_TODO_1),
                                        new ExecutedByHashed("EU:alice-hashed"),
                                        new ExecutedByEncoded("EU:aliceEncoded"),
                                        new EventType("TodoCreated"), new AggregateVersion(0)),
                                EncodedTraceIdentifiable.from(new MarkTodoAsDone(TodoId.USER_1_TODO_1),
                                        new ExecutedByHashed("EU:alice-hashed"),
                                        new ExecutedByEncoded("EU:aliceEncoded"),
                                        new EventType("TodoUpdated"), new AggregateVersion(1)),
                                EncodedTraceIdentifiable.from(new MarkTodoAsDone(TodoId.USER_1_TODO_1),
                                        new ExecutedByHashed("EU:alice-hashed"),
                                        new ExecutedByEncoded("EU:aliceEncoded"),
                                        new EventType("TodoUpdated"), new AggregateVersion(2)))),
                new TraceRecorder(
                        jdbcPostgresTraceIdGenerator.generate(),
                        new CorrelationId(2L),
                        new ExecutedAt(Instant.parse("2026-09-06T13:00:00Z")),
                        Source.COMMAND,
                        ExecutionStatus.FAILED_UNAUTHORIZED,
                        new From("fromCommand"),
                        List.of(
                                EncodedTraceIdentifiable.from(new MarkTodoAsDone(TodoId.USER_1_TODO_1),
                                        new ExecutedByHashed("EU:bob-hashed"),
                                        new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        jdbcPostgresTraceIdGenerator.generate(),
                        new CorrelationId(3L),
                        new ExecutedAt(Instant.parse("2026-09-06T14:00:00Z")),
                        Source.COMMAND,
                        ExecutionStatus.FAILED_BUSINESS,
                        new From("fromCommand"),
                        List.of(
                                EncodedTraceIdentifiable.from(new MarkTodoAsDone(TodoId.USER_1_TODO_1),
                                        new ExecutedByHashed("EU:bob-hashed"),
                                        new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        jdbcPostgresTraceIdGenerator.generate(),
                        new CorrelationId(4L),
                        new ExecutedAt(Instant.parse("2026-09-06T15:00:00Z")),
                        Source.QUERY,
                        ExecutionStatus.SUCCESS,
                        new From("fromQuery"),
                        List.of(
                                EncodedTraceIdentifiable.from(
                                        new TodoProjection(
                                                ProjectionId.from(TodoId.USER_1_TODO_2),
                                                new Description("lorem ipsum"),
                                                Status.IN_PROGRESS, false, List.of(), Owner.USER_1),
                                        new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        jdbcPostgresTraceIdGenerator.generate(),
                        new CorrelationId(5L),
                        new ExecutedAt(Instant.parse("2026-09-06T16:00:00Z")),
                        Source.QUERY,
                        ExecutionStatus.FAILED_UNAUTHORIZED,
                        new From("fromQuery"),
                        List.of(
                                EncodedTraceIdentifiable.from(
                                        new TodoProjection(
                                                ProjectionId.from(TodoId.USER_1_TODO_1),
                                                new Description("lorem ipsum"),
                                                Status.IN_PROGRESS, false, List.of(), Owner.USER_1),
                                        new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        jdbcPostgresTraceIdGenerator.generate(),
                        new CorrelationId(6L),
                        new ExecutedAt(Instant.parse("2026-09-06T17:00:00Z")),
                        Source.QUERY,
                        ExecutionStatus.FAILED_UNAUTHORIZED,
                        new From("fromQuery"),
                        List.of(
                                EncodedTraceIdentifiable.from(
                                        new TodoChecklistProjection(
                                                ProjectionId.from(TodoChecklistId.USER_1_TODO_1_1),
                                                "Implement Projection feature"),
                                        new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        jdbcPostgresTraceIdGenerator.generate(),
                        new CorrelationId(7L),
                        new ExecutedAt(Instant.parse("2026-09-06T18:00:00Z")),
                        Source.COMMAND,
                        ExecutionStatus.SUCCESS,
                        new From("fromCommand"),
                        List.of(
                                EncodedTraceIdentifiable.from(new MarkTodoAsDone(TodoId.USER_1_TODO_2),
                                        new ExecutedByHashed("EU:bob-hashed"),
                                        new ExecutedByEncoded("EU:bobEncoded"),
                                        new EventType("TodoCreated"), new AggregateVersion(0)))),
                new TraceRecorder(
                        jdbcPostgresTraceIdGenerator.generate(),
                        new CorrelationId(8L),
                        new ExecutedAt(Instant.parse("2026-09-06T19:00:00Z")),
                        Source.TRACEABILITY,
                        ExecutionStatus.SUCCESS,
                        new From("TRACEABILITY"),
                        List.of(
                                EncodedTraceIdentifiable.fromTraceability(TodoId.USER_1_TODO_2, AnyAggregateId.from(TodoId.USER_1_TODO_2),
                                        new ExecutedByHashed("EU:bob-hashed"),
                                        new ExecutedByEncoded("EU:bobEncoded")))),
                new TraceRecorder(
                        jdbcPostgresTraceIdGenerator.generate(),
                        new CorrelationId(9L),
                        new ExecutedAt(Instant.parse("2026-09-06T20:00:00Z")),
                        Source.TRACEABILITY,
                        ExecutionStatus.SUCCESS,
                        new From("TRACEABILITY"),
                        List.of(
                                EncodedTraceIdentifiable.fromTraceability(TodoId.USER_1_TODO_2, AnyAggregateId.from(TodoId.USER_1_TODO_2),
                                        new ExecutedByHashed("EU:bob-hashed"),
                                        new ExecutedByEncoded("EU:bobEncoded"))))
        )) {
            jdbcPostgresTraceRecorderRepository.store(traceRecorder);
        }
    }
}

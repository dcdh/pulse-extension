package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.query.Projection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EncodedTraceAggregateIdTest {

    private static final ExecutedByHashed EXECUTED_BY_HASHED = new ExecutedByHashed("A");
    private static final ExecutedByEncoded EXECUTED_BY_ENCODED = new ExecutedByEncoded("A");

    @Test
    void shouldCreateQueryTraceWithoutEvent() {
        // given
        final Projection<TodoId> projection = mock(Projection.class);
        final TodoId aggregateId = TodoId.USER_1_TODO_1;
        when(projection.id()).thenReturn(aggregateId);

        // when
        final EncodedTraceAggregateId trace = EncodedTraceAggregateId.from(
                projection, EXECUTED_BY_HASHED, EXECUTED_BY_ENCODED);

        // then
        assertAll(
                () -> assertEquals(Source.QUERY, trace.source()),
                () -> assertEquals(aggregateId, trace.aggregateId()),
                () -> assertEquals(EXECUTED_BY_HASHED, trace.executedByHashed()),
                () -> assertEquals(EXECUTED_BY_ENCODED, trace.executedByEncoded()),
                () -> assertNull(trace.eventType()),
                () -> assertNull(trace.aggregateVersion()),
                () -> assertFalse(trace.hasEvent())
        );
    }

    @Test
    void shouldCreateTraceabilityTraceWithoutEvent() {
        // given
        final TodoId aggregateId = TodoId.USER_1_TODO_1;

        // when
        final EncodedTraceAggregateId trace = EncodedTraceAggregateId.fromTraceability(
                aggregateId, EXECUTED_BY_HASHED, EXECUTED_BY_ENCODED);

        // then
        assertAll(
                () -> assertEquals(Source.TRACEABILITY, trace.source()),
                () -> assertEquals(aggregateId, trace.aggregateId()),
                () -> assertEquals(EXECUTED_BY_HASHED, trace.executedByHashed()),
                () -> assertEquals(EXECUTED_BY_ENCODED, trace.executedByEncoded()),
                () -> assertNull(trace.eventType()),
                () -> assertNull(trace.aggregateVersion()),
                () -> assertFalse(trace.hasEvent())
        );
    }

    @Test
    void shouldCreateCommandTraceWithEvent() {
        // given
        final Command<TodoId> command = new MarkTodoAsDone(TodoId.USER_1_TODO_1);
        final EventType eventType = new EventType("TodoCreated");
        final AggregateVersion aggregateVersion = new AggregateVersion(1);

        // when
        final EncodedTraceAggregateId trace = EncodedTraceAggregateId.from(
                command, EXECUTED_BY_HASHED, EXECUTED_BY_ENCODED, eventType, aggregateVersion);

        // then
        assertAll(
                () -> assertEquals(Source.COMMAND, trace.source()),
                () -> assertEquals(command.id(), trace.aggregateId()),
                () -> assertEquals(EXECUTED_BY_HASHED, trace.executedByHashed()),
                () -> assertEquals(EXECUTED_BY_ENCODED, trace.executedByEncoded()),
                () -> assertEquals(eventType, trace.eventType()),
                () -> assertEquals(aggregateVersion, trace.aggregateVersion()),
                () -> assertTrue(trace.hasEvent())
        );
    }

    @Test
    void shouldCreateCommandTraceWithoutEvent() {
        // given
        final Command<TodoId> command = new MarkTodoAsDone(TodoId.USER_1_TODO_1);

        // when
        final EncodedTraceAggregateId trace = EncodedTraceAggregateId.from(
                command, EXECUTED_BY_HASHED, EXECUTED_BY_ENCODED);

        // then
        assertAll(
                () -> assertEquals(Source.COMMAND, trace.source()),
                () -> assertEquals(command.id(), trace.aggregateId()),
                () -> assertEquals(EXECUTED_BY_HASHED, trace.executedByHashed()),
                () -> assertEquals(EXECUTED_BY_ENCODED, trace.executedByEncoded()),
                () -> assertNull(trace.eventType()),
                () -> assertNull(trace.aggregateVersion()),
                () -> assertFalse(trace.hasEvent())
        );
    }

    @Test
    void shouldReportNoEventWhenOnlyEventTypeIsPresent() {
        // given
        final Command<TodoId> command = new MarkTodoAsDone(TodoId.USER_1_TODO_1);
        final EventType eventType = new EventType("TodoCreated");

        // when
        final EncodedTraceAggregateId trace = EncodedTraceAggregateId.from(
                command, EXECUTED_BY_HASHED, EXECUTED_BY_ENCODED,
                eventType, null);

        // then
        assertFalse(trace.hasEvent());
    }

    @Test
    void shouldReportNoEventWhenOnlyAggregateVersionIsPresent() {
        // given
        final Command<TodoId> command = new MarkTodoAsDone(TodoId.USER_1_TODO_1);
        final AggregateVersion aggregateVersion = new AggregateVersion(1);

        // when
        final EncodedTraceAggregateId trace = EncodedTraceAggregateId.from(
                command, EXECUTED_BY_HASHED, EXECUTED_BY_ENCODED,
                null, aggregateVersion);

        // then
        assertFalse(trace.hasEvent());
    }

    @Test
    void shouldRejectEventFieldsForQuery() {
        // given
        final TodoId aggregateId = TodoId.USER_1_TODO_1;
        final EventType eventType = new EventType("TodoCreated");
        final AggregateVersion aggregateVersion = new AggregateVersion(1);

        // when / then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new EncodedTraceAggregateId(
                        Source.QUERY, aggregateId, EXECUTED_BY_HASHED, EXECUTED_BY_ENCODED,
                        eventType, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new EncodedTraceAggregateId(
                        Source.QUERY, aggregateId, EXECUTED_BY_HASHED, EXECUTED_BY_ENCODED,
                        null, aggregateVersion))
        );
    }

    @Test
    void shouldRequireSourceAggregateAndExecutionIdentities() {
        // given
        final TodoId aggregateId = TodoId.USER_1_TODO_1;

        // when / then
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new EncodedTraceAggregateId(
                        null, aggregateId, EXECUTED_BY_HASHED, EXECUTED_BY_ENCODED, null, null)),
                () -> assertThrows(NullPointerException.class, () -> new EncodedTraceAggregateId(
                        Source.COMMAND, null, EXECUTED_BY_HASHED, EXECUTED_BY_ENCODED, null, null)),
                () -> assertThrows(NullPointerException.class, () -> new EncodedTraceAggregateId(
                        Source.COMMAND, aggregateId, null, EXECUTED_BY_ENCODED, null, null)),
                () -> assertThrows(NullPointerException.class, () -> new EncodedTraceAggregateId(
                        Source.COMMAND, aggregateId, EXECUTED_BY_HASHED, null, null, null))
        );
    }
}

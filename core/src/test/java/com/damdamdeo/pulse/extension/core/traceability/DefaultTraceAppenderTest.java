package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.command.CommandId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.*;
import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultTraceAppenderTest {

    @Mock
    private ExecutionContextProvider executionContextProvider;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private ExecutedAtProvider executedAtProvider;

    @Mock
    private TraceIdGenerator traceIdGenerator;

    @Mock
    private CorrelationIdProvider correlationIdProvider;

    @Mock
    private UsernameHasher usernameHasher;

    @Mock
    private ExecutedByEncodedProvider executedByEncodedProvider;

    @Mock
    private TraceRecorderRepositoryProvider traceRecorderRepositoryProvider;

    @Mock
    private TraceRecorderRepository traceRecorderRepository;

    private DefaultTraceAppender traceAppender;

    @BeforeEach
    void setUp() {
        traceAppender = new DefaultTraceAppender(executionContextProvider, executedAtProvider, traceIdGenerator,
                correlationIdProvider, usernameHasher, executedByEncodedProvider, traceRecorderRepositoryProvider);
    }

    @Test
    void shouldAppendQueryTrace() throws Exception {
        // Given
        final TodoId firstAggregateId = TodoId.USER_1_TODO_1;
        final TodoId secondAggregateId = TodoId.USER_1_TODO_2;
        final TestInput input = new TestInput();
        final List<TestProjection> projections = List.of(
                new TestProjection(ProjectionId.from(firstAggregateId)),
                new TestProjection(ProjectionId.from(secondAggregateId)));
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final TraceId traceId = new TraceId(123L);
        final CorrelationId correlationId = new CorrelationId(125L);
        final ExecutedAt executedAt = new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"));
        final ExecutedByEncoded firstExecutedByEncoded = new ExecutedByEncoded("SA:encoded-1");
        final ExecutedByEncoded secondExecutedByEncoded = new ExecutedByEncoded("SA:encoded-2");
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenReturn(traceId);
        when(correlationIdProvider.provide()).thenReturn(correlationId);
        when(executedAtProvider.now()).thenReturn(executedAt);
        when(executedByEncodedProvider.provide(firstAggregateId, executedBy)).thenReturn(firstExecutedByEncoded);
        when(executedByEncodedProvider.provide(secondAggregateId, executedBy)).thenReturn(secondExecutedByEncoded);
        when(traceRecorderRepositoryProvider.provide(
                TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY)).thenReturn(traceRecorderRepository);

        // When
        traceAppender.append(input, projections, ExecutionStatus.SUCCESS, TraceabilityDetails.INVOLVED_ONLY);

        // Then
        final ArgumentCaptor<TraceRecorder> traceRecorderCaptor = ArgumentCaptor.forClass(TraceRecorder.class);
        verify(traceRecorderRepositoryProvider).provide(any());
        verify(traceRecorderRepository).store(traceRecorderCaptor.capture());
        final TraceRecorder traceRecorder = traceRecorderCaptor.getValue();
        assertAll(
                () -> assertSame(traceId, traceRecorder.traceId()),
                () -> assertSame(correlationId, traceRecorder.correlationId()),
                () -> assertSame(executedAt, traceRecorder.executedAt()),
                () -> assertEquals(Source.QUERY, traceRecorder.source()),
                () -> assertEquals(ExecutionStatus.SUCCESS, traceRecorder.executionStatus()),
                () -> assertEquals(From.from(input), traceRecorder.from()),
                () -> assertThat(traceRecorder.encodedTraceIdentifiables()).containsExactly(
                        EncodedTraceIdentifiable.from(projections.getFirst(), new ExecutedByHashed("A"), firstExecutedByEncoded),
                        EncodedTraceIdentifiable.from(projections.get(1), new ExecutedByHashed("A"), secondExecutedByEncoded)),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(correlationIdProvider).provide(),
                () -> verify(executedAtProvider).now(),
                () -> verify(executedByEncodedProvider).provide(firstAggregateId, executedBy),
                () -> verify(executedByEncodedProvider).provide(secondAggregateId, executedBy)
        );
    }

    @Test
    void shouldAppendTraceabilityTrace() throws Exception {
        // Given
        final Identifiable firstIdentifiable = mock(Identifiable.class);
        final Identifiable secondIdentifiable = mock(Identifiable.class);
        final Map<Identifiable, AnyAggregateId> aggregateIds = new LinkedHashMap<>();
        aggregateIds.put(firstIdentifiable, new AnyAggregateId("aggregate-1"));
        aggregateIds.put(secondIdentifiable, new AnyAggregateId("aggregate-2"));
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final TraceId traceId = new TraceId(123L);
        final CorrelationId correlationId = new CorrelationId(125L);
        final ExecutedAt executedAt = new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"));
        final ExecutedByEncoded firstExecutedByEncoded = new ExecutedByEncoded("SA:encoded-1");
        final ExecutedByEncoded secondExecutedByEncoded = new ExecutedByEncoded("SA:encoded-2");
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenReturn(traceId);
        when(correlationIdProvider.provide()).thenReturn(correlationId);
        when(executedAtProvider.now()).thenReturn(executedAt);
        when(executedByEncodedProvider.provide(firstIdentifiable, executedBy)).thenReturn(firstExecutedByEncoded);
        when(executedByEncodedProvider.provide(secondIdentifiable, executedBy)).thenReturn(secondExecutedByEncoded);
        when(traceRecorderRepositoryProvider.provide(
                TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY)).thenReturn(traceRecorderRepository);

        // When
        traceAppender.append(aggregateIds);

        // Then
        final ArgumentCaptor<TraceRecorder> traceRecorderCaptor = ArgumentCaptor.forClass(TraceRecorder.class);
        verify(traceRecorderRepositoryProvider).provide(any());
        verify(traceRecorderRepository).store(traceRecorderCaptor.capture());
        final TraceRecorder traceRecorder = traceRecorderCaptor.getValue();
        assertAll(
                () -> assertSame(traceId, traceRecorder.traceId()),
                () -> assertSame(correlationId, traceRecorder.correlationId()),
                () -> assertSame(executedAt, traceRecorder.executedAt()),
                () -> assertEquals(Source.TRACEABILITY, traceRecorder.source()),
                () -> assertEquals(ExecutionStatus.SUCCESS, traceRecorder.executionStatus()),
                () -> assertEquals(new From("TRACEABILITY"), traceRecorder.from()),
                () -> assertThat(traceRecorder.encodedTraceIdentifiables()).containsExactly(
                        EncodedTraceIdentifiable.fromTraceability(firstIdentifiable, aggregateIds.get(firstIdentifiable), new ExecutedByHashed("A"), firstExecutedByEncoded),
                        EncodedTraceIdentifiable.fromTraceability(secondIdentifiable, aggregateIds.get(secondIdentifiable), new ExecutedByHashed("A"), secondExecutedByEncoded)),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(correlationIdProvider).provide(),
                () -> verify(executedAtProvider).now(),
                () -> verify(executedByEncodedProvider).provide(firstIdentifiable, executedBy),
                () -> verify(executedByEncodedProvider).provide(secondIdentifiable, executedBy)
        );
    }

    @Test
    void shouldNotAppendTraceabilityTraceWhenThereAreNoAggregateIds() throws Exception {
        // When
        traceAppender.append(Map.of());

        // Then
        verifyNoInteractions(executionContextProvider, executedAtProvider, traceIdGenerator, usernameHasher,
                correlationIdProvider, executedByEncodedProvider, traceRecorderRepositoryProvider);
    }

    @Test
    void shouldWrapTraceabilityTraceIdGenerationFailure() throws Exception {
        // Given
        final TodoId aggregateId = TodoId.USER_1_TODO_1;
        final TraceIdGeneratorException cause = new TraceIdGeneratorException(new RuntimeException("Unable to generate trace id"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(ExecutedBy.Anonymous.INSTANCE);
        when(traceIdGenerator.generate()).thenThrow(cause);
        when(executedByEncodedProvider.provide(any(), any())).thenReturn(new ExecutedByEncoded("SA:encoded"));

        // When
        final TraceAppenderException exception = assertThrows(TraceAppenderException.class,
                () -> traceAppender.append(Map.of(aggregateId, new AnyAggregateId(aggregateId.id()))));

        // Then
        assertSame(cause, exception.getCause());
        verify(traceRecorderRepositoryProvider).provide(
                TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY);
        verifyNoInteractions(correlationIdProvider, executedAtProvider);
    }

    @Test
    void shouldWrapTraceabilityTraceStorageFailure() throws Exception {
        // Given
        final TodoId aggregateId = TodoId.USER_1_TODO_1;
        final TraceRepositoryException cause = new TraceRepositoryException(new RuntimeException("Unable to store trace"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(ExecutedBy.Anonymous.INSTANCE);
        when(traceIdGenerator.generate()).thenReturn(new TraceId(123L));
        when(correlationIdProvider.provide()).thenReturn(new CorrelationId(125L));
        when(executedAtProvider.now()).thenReturn(new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")));
        when(executedByEncodedProvider.provide(any(), any())).thenReturn(new ExecutedByEncoded("SA:encoded"));
        when(traceRecorderRepositoryProvider.provide(
                TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY)).thenReturn(traceRecorderRepository);
        doThrow(cause).when(traceRecorderRepository).store(any(TraceRecorder.class));

        // When
        final TraceAppenderException exception = assertThrows(TraceAppenderException.class,
                () -> traceAppender.append(Map.of(aggregateId, new AnyAggregateId(aggregateId.id()))));

        // Then
        assertSame(cause, exception.getCause());
        verify(traceRecorderRepository).store(any(TraceRecorder.class));
    }

    @Test
    void shouldAppendCommandTraceForEachVersionizedEvent() throws Exception {
        // Given
        final TodoId todoId = TodoId.USER_1_TODO_1;;
        final TestCommand command = new TestCommand(todoId);
        final AggregateVersion firstVersion = new AggregateVersion(1);
        final AggregateVersion secondVersion = new AggregateVersion(2);
        final List<VersionizedEvent<TodoId>> versionizedEvents = List.of(
                new VersionizedEvent<>(firstVersion,
                        new ExecutedByEvent<>(new FirstTestEvent(), ExecutedBy.NotAvailable.INSTANCE)),
                new VersionizedEvent<>(secondVersion,
                        new ExecutedByEvent<>(new SecondTestEvent(), ExecutedBy.NotAvailable.INSTANCE))
        );
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final TraceId traceId = new TraceId(123L);
        final CorrelationId correlationId = new CorrelationId(125L);
        final ExecutedAt executedAt = new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"));
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("SA:encoded");
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenReturn(traceId);
        when(correlationIdProvider.provide()).thenReturn(correlationId);
        when(executedAtProvider.now()).thenReturn(executedAt);
        when(executedByEncodedProvider.provide(todoId, executedBy)).thenReturn(executedByEncoded);
        when(traceRecorderRepositoryProvider.provide(
                TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY)).thenReturn(traceRecorderRepository);

        // When
        traceAppender.append(command, versionizedEvents, ExecutionStatus.FAILED_BUSINESS);

        // Then
        final ArgumentCaptor<TraceRecorder> traceRecorderCaptor = ArgumentCaptor.forClass(TraceRecorder.class);
        verify(traceRecorderRepositoryProvider).provide(any());
        verify(traceRecorderRepository).store(traceRecorderCaptor.capture());
        final TraceRecorder traceRecorder = traceRecorderCaptor.getValue();
        assertAll(
                () -> assertSame(traceId, traceRecorder.traceId()),
                () -> assertSame(correlationId, traceRecorder.correlationId()),
                () -> assertSame(executedAt, traceRecorder.executedAt()),
                () -> assertEquals(Source.COMMAND, traceRecorder.source()),
                () -> assertEquals(ExecutionStatus.FAILED_BUSINESS, traceRecorder.executionStatus()),
                () -> assertEquals(From.from(command), traceRecorder.from()),
                () -> assertThat(traceRecorder.encodedTraceIdentifiables()).containsExactly(
                        EncodedTraceIdentifiable.from(
                                command,
                                new ExecutedByHashed("A"),
                                executedByEncoded,
                                EventType.from(FirstTestEvent.class),
                                firstVersion
                        ),
                        EncodedTraceIdentifiable.from(
                                command,
                                new ExecutedByHashed("A"),
                                executedByEncoded,
                                EventType.from(SecondTestEvent.class),
                                secondVersion
                        )
                ),
                () -> verify(executedByEncodedProvider, times(2)).provide(todoId, executedBy)
        );
    }

    @Test
    void shouldAppendCommandTraceWhenThereAreNoVersionizedEvents() throws Exception {
        // Given
        final TodoId todoId = TodoId.USER_1_TODO_1;
        final TestCommand command = new TestCommand(todoId);
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final TraceId traceId = new TraceId(123L);
        final CorrelationId correlationId = new CorrelationId(125L);
        final ExecutedAt executedAt = new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"));
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("SA:encoded");
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenReturn(traceId);
        when(correlationIdProvider.provide()).thenReturn(correlationId);
        when(executedAtProvider.now()).thenReturn(executedAt);
        when(executedByEncodedProvider.provide(todoId, executedBy)).thenReturn(executedByEncoded);
        when(traceRecorderRepositoryProvider.provide(
                TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY)).thenReturn(traceRecorderRepository);

        // When
        traceAppender.append(command, List.of(), ExecutionStatus.SUCCESS);

        // Then
        final ArgumentCaptor<TraceRecorder> traceRecorderCaptor = ArgumentCaptor.forClass(TraceRecorder.class);
        verify(traceRecorderRepositoryProvider).provide(any());
        verify(traceRecorderRepository).store(traceRecorderCaptor.capture());
        final TraceRecorder traceRecorder = traceRecorderCaptor.getValue();
        assertAll(
                () -> assertSame(traceId, traceRecorder.traceId()),
                () -> assertSame(correlationId, traceRecorder.correlationId()),
                () -> assertSame(executedAt, traceRecorder.executedAt()),
                () -> assertEquals(Source.COMMAND, traceRecorder.source()),
                () -> assertEquals(ExecutionStatus.SUCCESS, traceRecorder.executionStatus()),
                () -> assertEquals(From.from(command), traceRecorder.from()),
                () -> assertThat(traceRecorder.encodedTraceIdentifiables()).containsExactly(
                        EncodedTraceIdentifiable.from(command, new ExecutedByHashed("A"),
                                executedByEncoded)),
                () -> verify(executedByEncodedProvider).provide(todoId, executedBy)
        );
    }

    @Test
    void shouldNotAppendTraceWhenThereAreNoAggregateIds() throws Exception {
        // Given
        final TestInput input = new TestInput();

        // When
        traceAppender.append(input, List.of(), ExecutionStatus.SUCCESS, TraceabilityDetails.INVOLVED_ONLY);

        // Then
        verifyNoInteractions(executionContextProvider, executedAtProvider, traceIdGenerator, usernameHasher,
                correlationIdProvider, executedByEncodedProvider, traceRecorderRepositoryProvider);
    }

    @Test
    void shouldThrowTraceAppenderExceptionWhenTraceIdCannotBeGenerated() throws Exception {
        // Given
        final TodoId todoId = TodoId.USER_1_TODO_1;
        final TestInput input = new TestInput();
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("SA:encoded-1");
        final TraceIdGeneratorException cause = new TraceIdGeneratorException(new RuntimeException("Unable to store trace"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenThrow(cause);
        when(executedByEncodedProvider.provide(todoId, executedBy)).thenReturn(executedByEncoded);

        // When
        final TraceAppenderException exception = assertThrows(
                TraceAppenderException.class,
                () -> traceAppender.append(input, List.of(
                                new TestProjection(ProjectionId.from(todoId))), ExecutionStatus.SUCCESS,
                        TraceabilityDetails.INVOLVED_ONLY));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(executedByEncodedProvider).provide(any(), any()),
                () -> verify(traceRecorderRepositoryProvider).provide(
                        TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY),
                () -> verifyNoInteractions(correlationIdProvider, executedAtProvider, usernameHasher)
        );
    }

    @Test
    void shouldThrowTraceAppenderExceptionWhenCorrelationIdCannotBeGenerated() throws Exception {
        // Given
        final TodoId todoId = TodoId.USER_1_TODO_1;
        final TestInput input = new TestInput();
        final TraceId traceId = new TraceId(123L);
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("SA:encoded-1");
        final CorrelationIdProviderException cause = new CorrelationIdProviderException(new RuntimeException("Unable to store trace"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenReturn(traceId);
        when(correlationIdProvider.provide()).thenThrow(cause);
        when(executedByEncodedProvider.provide(todoId, executedBy)).thenReturn(executedByEncoded);

        // When
        final TraceAppenderException exception = assertThrows(
                TraceAppenderException.class,
                () -> traceAppender.append(input, List.of(
                                new TestProjection(ProjectionId.from(todoId))), ExecutionStatus.SUCCESS,
                        TraceabilityDetails.INVOLVED_ONLY));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(correlationIdProvider).provide(),
                () -> verify(executedByEncodedProvider).provide(any(), any()),
                () -> verify(traceRecorderRepositoryProvider).provide(
                        TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY),
                () -> verifyNoInteractions(executedAtProvider, usernameHasher)
        );
    }

    @Test
    void shouldThrowTraceAppenderExceptionWhenTraceCannotBeStored() throws Exception {
        // Given
        final TodoId todoId = TodoId.USER_1_TODO_1;
        final TestInput input = new TestInput();
        final TraceId traceId = new TraceId(123L);
        final CorrelationId correlationId = new CorrelationId(125L);
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final ExecutedAt executedAt = new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"));
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("SA:encoded-1");
        final TraceRepositoryException cause = new TraceRepositoryException(new RuntimeException("Unable to store trace"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenReturn(traceId);
        when(correlationIdProvider.provide()).thenReturn(correlationId);
        when(executedAtProvider.now()).thenReturn(executedAt);
        when(executedByEncodedProvider.provide(todoId, executedBy)).thenReturn(executedByEncoded);
        when(traceRecorderRepositoryProvider.provide(
                TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY)).thenReturn(traceRecorderRepository);
        doThrow(cause).when(traceRecorderRepository).store(any(TraceRecorder.class));

        // When
        final TraceAppenderException exception = assertThrows(
                TraceAppenderException.class,
                () -> traceAppender.append(input, List.of(
                                new TestProjection(ProjectionId.from(todoId))), ExecutionStatus.SUCCESS,
                        TraceabilityDetails.INVOLVED_ONLY));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(traceRecorderRepositoryProvider).provide(any()),
                () -> verify(traceRecorderRepository).store(any(TraceRecorder.class)),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(correlationIdProvider).provide(),
                () -> verify(executedAtProvider).now(),
                () -> verify(executedByEncodedProvider).provide(any(), any())
        );
    }

    private record TestCommand(TodoId testId) implements Command<TodoId> {

        @Override
        public CommandId<TodoId> id() {
            return CommandId.from(testId);
        }
    }

    private record TestInput() implements Input {
    }

    private record TestProjection(ProjectionId<TodoId> id) implements Projection<TodoId> {
    }

    private record FirstTestEvent() implements Event<TodoId> {
    }

    private record SecondTestEvent() implements Event<TodoId> {
    }
}

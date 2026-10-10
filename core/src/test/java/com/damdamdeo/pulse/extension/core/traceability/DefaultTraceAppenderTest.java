package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.ExecutedByEvent;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.executedby.*;
import com.damdamdeo.pulse.extension.core.query.Input;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private TraceRecorderRepository traceRecorderRepository;

    private DefaultTraceAppender traceAppender;

    @BeforeEach
    void setUp() {
        traceAppender = new DefaultTraceAppender(executionContextProvider, executedAtProvider, traceIdGenerator,
                correlationIdProvider, usernameHasher, executedByEncodedProvider, traceRecorderRepository);
    }

    @Test
    void shouldAppendQueryTrace() throws Exception {
        // Given
        final AggregateId firstAggregateId = mock(AggregateId.class);
        final AggregateId secondAggregateId = mock(AggregateId.class);
        final TestInput input = new TestInput();
        final LinkedHashSet<AggregateId> aggregateIds = new LinkedHashSet<>();
        aggregateIds.add(firstAggregateId);
        aggregateIds.add(secondAggregateId);
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

        // When
        traceAppender.append(input, aggregateIds, ExecutionStatus.SUCCESS);

        // Then
        final ArgumentCaptor<TraceRecorder> traceRecorderCaptor = ArgumentCaptor.forClass(TraceRecorder.class);
        verify(traceRecorderRepository).store(traceRecorderCaptor.capture());
        final TraceRecorder traceRecorder = traceRecorderCaptor.getValue();
        assertAll(
                () -> assertSame(traceId, traceRecorder.traceId()),
                () -> assertSame(correlationId, traceRecorder.correlationId()),
                () -> assertSame(executedAt, traceRecorder.executedAt()),
                () -> assertEquals(Source.QUERY, traceRecorder.source()),
                () -> assertEquals(ExecutionStatus.SUCCESS, traceRecorder.executionStatus()),
                () -> assertEquals(From.from(input), traceRecorder.from()),
                () -> assertThat(traceRecorder.encodedTraceAggregateIds()).containsExactly(
                        EncodedTraceAggregateId.fromQuery(firstAggregateId, new ExecutedByHashed("A"), firstExecutedByEncoded),
                        EncodedTraceAggregateId.fromQuery(secondAggregateId, new ExecutedByHashed("A"), secondExecutedByEncoded)),
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
        final AggregateId firstAggregateId = mock(AggregateId.class);
        final AggregateId secondAggregateId = mock(AggregateId.class);
        final LinkedHashSet<AggregateId> aggregateIds = new LinkedHashSet<>(List.of(firstAggregateId, secondAggregateId));
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

        // When
        traceAppender.append(aggregateIds);

        // Then
        final ArgumentCaptor<TraceRecorder> traceRecorderCaptor = ArgumentCaptor.forClass(TraceRecorder.class);
        verify(traceRecorderRepository).store(traceRecorderCaptor.capture());
        final TraceRecorder traceRecorder = traceRecorderCaptor.getValue();
        assertAll(
                () -> assertSame(traceId, traceRecorder.traceId()),
                () -> assertSame(correlationId, traceRecorder.correlationId()),
                () -> assertSame(executedAt, traceRecorder.executedAt()),
                () -> assertEquals(Source.TRACEABILITY, traceRecorder.source()),
                () -> assertEquals(ExecutionStatus.SUCCESS, traceRecorder.executionStatus()),
                () -> assertEquals(new From("TRACEABILITY"), traceRecorder.from()),
                () -> assertThat(traceRecorder.encodedTraceAggregateIds()).containsExactly(
                        EncodedTraceAggregateId.fromTraceability(firstAggregateId, new ExecutedByHashed("A"), firstExecutedByEncoded),
                        EncodedTraceAggregateId.fromTraceability(secondAggregateId, new ExecutedByHashed("A"), secondExecutedByEncoded)),
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
    void shouldNotAppendTraceabilityTraceWhenThereAreNoAggregateIds() throws Exception {
        // When
        traceAppender.append(Set.of());

        // Then
        verifyNoInteractions(executionContextProvider, executedAtProvider, traceIdGenerator, usernameHasher,
                correlationIdProvider, executedByEncodedProvider, traceRecorderRepository);
    }

    @Test
    void shouldWrapTraceabilityTraceIdGenerationFailure() throws Exception {
        // Given
        final AggregateId aggregateId = mock(AggregateId.class);
        final TraceIdGeneratorException cause = new TraceIdGeneratorException(new RuntimeException("Unable to generate trace id"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(ExecutedBy.Anonymous.INSTANCE);
        when(traceIdGenerator.generate()).thenThrow(cause);
        when(executedByEncodedProvider.provide(any(), any())).thenReturn(new ExecutedByEncoded("SA:encoded"));

        // When
        final TraceAppenderException exception = assertThrows(TraceAppenderException.class,
                () -> traceAppender.append(Set.of(aggregateId)));

        // Then
        assertSame(cause, exception.getCause());
        verifyNoInteractions(correlationIdProvider, executedAtProvider, traceRecorderRepository);
    }

    @Test
    void shouldWrapTraceabilityTraceStorageFailure() throws Exception {
        // Given
        final AggregateId aggregateId = mock(AggregateId.class);
        final TraceRepositoryException cause = new TraceRepositoryException(new RuntimeException("Unable to store trace"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(ExecutedBy.Anonymous.INSTANCE);
        when(traceIdGenerator.generate()).thenReturn(new TraceId(123L));
        when(correlationIdProvider.provide()).thenReturn(new CorrelationId(125L));
        when(executedAtProvider.now()).thenReturn(new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")));
        when(executedByEncodedProvider.provide(any(), any())).thenReturn(new ExecutedByEncoded("SA:encoded"));
        doThrow(cause).when(traceRecorderRepository).store(any(TraceRecorder.class));

        // When
        final TraceAppenderException exception = assertThrows(TraceAppenderException.class,
                () -> traceAppender.append(Set.of(aggregateId)));

        // Then
        assertSame(cause, exception.getCause());
        verify(traceRecorderRepository).store(any(TraceRecorder.class));
    }

    @Test
    void shouldAppendCommandTraceForEachVersionizedEvent() throws Exception {
        // Given
        final TestId aggregateId = new TestId("aggregate-id");
        final TestCommand command = new TestCommand(aggregateId);
        final AggregateVersion firstVersion = new AggregateVersion(1);
        final AggregateVersion secondVersion = new AggregateVersion(2);
        final List<VersionizedEvent<TestId>> versionizedEvents = List.of(
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
        when(executedByEncodedProvider.provide(aggregateId, executedBy)).thenReturn(executedByEncoded);

        // When
        traceAppender.append(command, versionizedEvents, ExecutionStatus.FAILED_BUSINESS);

        // Then
        final ArgumentCaptor<TraceRecorder> traceRecorderCaptor = ArgumentCaptor.forClass(TraceRecorder.class);
        verify(traceRecorderRepository).store(traceRecorderCaptor.capture());
        final TraceRecorder traceRecorder = traceRecorderCaptor.getValue();
        assertAll(
                () -> assertSame(traceId, traceRecorder.traceId()),
                () -> assertSame(correlationId, traceRecorder.correlationId()),
                () -> assertSame(executedAt, traceRecorder.executedAt()),
                () -> assertEquals(Source.COMMAND, traceRecorder.source()),
                () -> assertEquals(ExecutionStatus.FAILED_BUSINESS, traceRecorder.executionStatus()),
                () -> assertEquals(From.from(command), traceRecorder.from()),
                () -> assertThat(traceRecorder.encodedTraceAggregateIds()).containsExactly(
                        EncodedTraceAggregateId.fromCommand(
                                aggregateId,
                                new ExecutedByHashed("A"),
                                executedByEncoded,
                                EventType.from(FirstTestEvent.class),
                                firstVersion
                        ),
                        EncodedTraceAggregateId.fromCommand(
                                aggregateId,
                                new ExecutedByHashed("A"),
                                executedByEncoded,
                                EventType.from(SecondTestEvent.class),
                                secondVersion
                        )
                ),
                () -> verify(executedByEncodedProvider, times(2)).provide(aggregateId, executedBy)
        );
    }

    @Test
    void shouldAppendCommandTraceWhenThereAreNoVersionizedEvents() throws Exception {
        // Given
        final TestId aggregateId = new TestId("aggregate-id");
        final TestCommand command = new TestCommand(aggregateId);
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
        when(executedByEncodedProvider.provide(aggregateId, executedBy)).thenReturn(executedByEncoded);

        // When
        traceAppender.append(command, List.of(), ExecutionStatus.SUCCESS);

        // Then
        final ArgumentCaptor<TraceRecorder> traceRecorderCaptor = ArgumentCaptor.forClass(TraceRecorder.class);
        verify(traceRecorderRepository).store(traceRecorderCaptor.capture());
        final TraceRecorder traceRecorder = traceRecorderCaptor.getValue();
        assertAll(
                () -> assertSame(traceId, traceRecorder.traceId()),
                () -> assertSame(correlationId, traceRecorder.correlationId()),
                () -> assertSame(executedAt, traceRecorder.executedAt()),
                () -> assertEquals(Source.COMMAND, traceRecorder.source()),
                () -> assertEquals(ExecutionStatus.SUCCESS, traceRecorder.executionStatus()),
                () -> assertEquals(From.from(command), traceRecorder.from()),
                () -> assertThat(traceRecorder.encodedTraceAggregateIds()).containsExactly(
                        EncodedTraceAggregateId.fromCommand(aggregateId, new ExecutedByHashed("A"),
                                executedByEncoded)),
                () -> verify(executedByEncodedProvider).provide(aggregateId, executedBy)
        );
    }

    @Test
    void shouldNotAppendTraceWhenThereAreNoAggregateIds() throws Exception {
        // Given
        final TestInput input = new TestInput();

        // When
        traceAppender.append(input, Set.of(), ExecutionStatus.SUCCESS);

        // Then
        verifyNoInteractions(executionContextProvider, executedAtProvider, traceIdGenerator, usernameHasher,
                correlationIdProvider, executedByEncodedProvider, traceRecorderRepository);
    }

    @Test
    void shouldThrowTraceAppenderExceptionWhenTraceIdCannotBeGenerated() throws Exception {
        // Given
        final AggregateId aggregateId = mock(AggregateId.class);
        final TestInput input = new TestInput();
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("SA:encoded-1");
        final TraceIdGeneratorException cause = new TraceIdGeneratorException(new RuntimeException("Unable to store trace"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenThrow(cause);
        when(executedByEncodedProvider.provide(aggregateId, executedBy)).thenReturn(executedByEncoded);

        // When
        final TraceAppenderException exception = assertThrows(
                TraceAppenderException.class,
                () -> traceAppender.append(input, Set.of(aggregateId), ExecutionStatus.SUCCESS));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(executedByEncodedProvider).provide(any(), any()),
                () -> verifyNoInteractions(correlationIdProvider, executedAtProvider, usernameHasher, traceRecorderRepository)
        );
    }

    @Test
    void shouldThrowTraceAppenderExceptionWhenCorrelationIdCannotBeGenerated() throws Exception {
        // Given
        final AggregateId aggregateId = mock(AggregateId.class);
        final TestInput input = new TestInput();
        final TraceId traceId = new TraceId(123L);
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("SA:encoded-1");
        final CorrelationIdProviderException cause = new CorrelationIdProviderException(new RuntimeException("Unable to store trace"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenReturn(traceId);
        when(correlationIdProvider.provide()).thenThrow(cause);
        when(executedByEncodedProvider.provide(aggregateId, executedBy)).thenReturn(executedByEncoded);

        // When
        final TraceAppenderException exception = assertThrows(
                TraceAppenderException.class,
                () -> traceAppender.append(input, Set.of(aggregateId), ExecutionStatus.SUCCESS));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(correlationIdProvider).provide(),
                () -> verify(executedByEncodedProvider).provide(any(), any()),
                () -> verifyNoInteractions(executedAtProvider, usernameHasher, traceRecorderRepository)
        );
    }

    @Test
    void shouldThrowTraceAppenderExceptionWhenTraceCannotBeStored() throws Exception {
        // Given
        final AggregateId aggregateId = mock(AggregateId.class);
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
        when(executedByEncodedProvider.provide(aggregateId, executedBy)).thenReturn(executedByEncoded);
        doThrow(cause).when(traceRecorderRepository).store(any(TraceRecorder.class));

        // When
        final TraceAppenderException exception = assertThrows(
                TraceAppenderException.class,
                () -> traceAppender.append(input, Set.of(aggregateId), ExecutionStatus.SUCCESS));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(traceRecorderRepository).store(any(TraceRecorder.class)),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(correlationIdProvider).provide(),
                () -> verify(executedAtProvider).now(),
                () -> verify(executedByEncodedProvider).provide(any(), any())
        );
    }

    private record TestId(String id) implements AggregateId {
    }

    private record TestCommand(TestId id) implements Command<TestId> {
    }

    private record TestInput() implements Input {
    }

    private record FirstTestEvent() implements Event<TestId> {
    }

    private record SecondTestEvent() implements Event<TestId> {
    }
}

package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashSet;
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
    private UsernameHasher usernameHasher;

    @Mock
    private ExecutedByEncodedProvider executedByEncodedProvider;

    @Mock
    private TraceRecorderRepository traceRecorderRepository;

    private DefaultTraceAppender traceAppender;

    @BeforeEach
    void setUp() {
        traceAppender = new DefaultTraceAppender(executionContextProvider, executedAtProvider, traceIdGenerator,
                usernameHasher, executedByEncodedProvider, traceRecorderRepository);
    }

    @Test
    void shouldAppendTrace() throws Exception {
        // Given
        final AggregateId firstAggregateId = mock(AggregateId.class);
        final AggregateId secondAggregateId = mock(AggregateId.class);
        final Traceable traceable = new Traceable() {
            @Override
            public Set<AggregateId> aggregateIds() {
                final LinkedHashSet<AggregateId> aggregateIds = new LinkedHashSet<>();
                aggregateIds.add(firstAggregateId);
                aggregateIds.add(secondAggregateId);
                return aggregateIds;
            }
        };
        final From from = new From("TestService");
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final TraceId traceId = new TraceId(123L);
        final ExecutedAt executedAt = new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"));
        final ExecutedByEncoded firstExecutedByEncoded = new ExecutedByEncoded("SA:encoded-1");
        final ExecutedByEncoded secondExecutedByEncoded = new ExecutedByEncoded("SA:encoded-2");
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenReturn(traceId);
        when(executedAtProvider.now()).thenReturn(executedAt);
        when(executedByEncodedProvider.provide(firstAggregateId, executedBy)).thenReturn(firstExecutedByEncoded);
        when(executedByEncodedProvider.provide(secondAggregateId, executedBy)).thenReturn(secondExecutedByEncoded);

        // When
        traceAppender.append(traceable, Source.COMMAND, from);

        // Then
        final ArgumentCaptor<TraceRecorder> traceRecorderCaptor = ArgumentCaptor.forClass(TraceRecorder.class);
        verify(traceRecorderRepository).store(traceRecorderCaptor.capture());
        final TraceRecorder traceRecorder = traceRecorderCaptor.getValue();
        assertAll(
                () -> assertSame(traceId, traceRecorder.traceId()),
                () -> assertSame(executedAt, traceRecorder.executedAt()),
                () -> assertSame(from, traceRecorder.from()),
                () -> assertThat(traceRecorder.encodedTraceAggregateIds()).containsExactly(
                        new EncodedTraceAggregateId(firstAggregateId, new ExecutedByHashed("A"), firstExecutedByEncoded),
                        new EncodedTraceAggregateId(secondAggregateId, new ExecutedByHashed("A"), secondExecutedByEncoded)),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(executedAtProvider).now(),
                () -> verify(executedByEncodedProvider).provide(firstAggregateId, executedBy),
                () -> verify(executedByEncodedProvider).provide(secondAggregateId, executedBy)
        );
    }

    @Test
    void shouldNotAppendTraceWhenThereAreNoAggregateIds() throws Exception {
        // Given
        final Traceable traceable = new Traceable() {
        };

        // When
        traceAppender.append(traceable, Source.COMMAND, new From("TestService"));

        // Then
        verifyNoInteractions(executionContextProvider, executedAtProvider, traceIdGenerator, usernameHasher,
                executedByEncodedProvider, traceRecorderRepository);
    }

    @Test
    void shouldThrowTraceAppenderExceptionWhenTraceIdCannotBeGenerated() throws Exception {
        // Given
        final AggregateId aggregateId = mock(AggregateId.class);
        final Traceable traceable = new Traceable() {
            @Override
            public Set<AggregateId> aggregateIds() {
                return Set.of(aggregateId);
            }
        };
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
                () -> traceAppender.append(traceable, Source.COMMAND, new From("TestService")));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(executedByEncodedProvider).provide(any(), any()),
                () -> verifyNoInteractions(executedAtProvider, usernameHasher, traceRecorderRepository)
        );
    }

    @Test
    void shouldThrowTraceAppenderExceptionWhenTraceCannotBeStored() throws Exception {
        // Given
        final AggregateId aggregateId = mock(AggregateId.class);
        final Traceable traceable = new Traceable() {
            @Override
            public Set<AggregateId> aggregateIds() {
                return Set.of(aggregateId);
            }
        };
        final TraceId traceId = new TraceId(123L);
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;
        final ExecutedAt executedAt = new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"));
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("SA:encoded-1");
        final TraceRepositoryException cause = new TraceRepositoryException(new RuntimeException("Unable to store trace"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(traceIdGenerator.generate()).thenReturn(traceId);
        when(executedAtProvider.now()).thenReturn(executedAt);
        when(executedByEncodedProvider.provide(aggregateId, executedBy)).thenReturn(executedByEncoded);
        doThrow(cause).when(traceRecorderRepository).store(any(TraceRecorder.class));

        // When
        final TraceAppenderException exception = assertThrows(
                TraceAppenderException.class,
                () -> traceAppender.append(traceable, Source.COMMAND, new From("TestService")));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(traceRecorderRepository).store(any(TraceRecorder.class)),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy(),
                () -> verify(traceIdGenerator).generate(),
                () -> verify(executedAtProvider).now(),
                () -> verify(executedByEncodedProvider).provide(any(), any())
        );
    }
}


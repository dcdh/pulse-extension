package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TargetEventChannelConsumerTest {

    private static class TodoTargetEventChannelConsumer extends AbstractTargetEventChannelConsumer<Todo> {

        public TodoTargetEventChannelConsumer(final TargetEventChannelExecutor<Todo> targetEventChannelExecutor,
                                              final IdempotencyRepository idempotencyRepository) {
            super(targetEventChannelExecutor, idempotencyRepository);
        }

    }

    record TodoEventKey(String aggregateRootType,
                        String aggregateRootId,
                        Integer version) implements EventKey {

        public static TodoEventKey of() {
            return new TodoEventKey(Todo.class.getName(), new TodoId("Damien", 0L).toString(), 1);
        }

        @Override
        public AggregateRootType toAggregateRootType() {
            return new AggregateRootType(aggregateRootType);
        }

        @Override
        public AggregateId toAggregateId() {
            return new AnyAggregateId(aggregateRootId);
        }

        @Override
        public CurrentVersionInConsumption toCurrentVersionInConsumption() {
            return new CurrentVersionInConsumption(version);
        }
    }

    record TodoEventRecord(String aggregateRootType,
                           String aggregateRootId,
                           Integer version,
                           Long createDate,
                           String eventType,
                           byte[] eventPayload,
                           String ownedBy) implements EventRecord {

        public static TodoEventRecord of() {
            return new TodoEventRecord(
                    Todo.class.getName(), new TodoId("Damien", 0L).toString(), 1,
                    1983L, NewTodoCreated.class.getName(), "eventPayload".getBytes(StandardCharsets.UTF_8),
                    "Damien");
        }

        @Override
        public AggregateRootType toAggregateRootType() {
            return new AggregateRootType(aggregateRootType);
        }

        @Override
        public AggregateId toAggregateId() {
            return new AnyAggregateId(aggregateRootId);
        }

        @Override
        public CurrentVersionInConsumption toCurrentVersionInConsumption() {
            return new CurrentVersionInConsumption(version);
        }

        @Override
        public Instant toCreationDate() {
            return Instant.ofEpochMilli(createDate);
        }

        @Override
        public EventType toEventType() {
            return new EventType(eventType);
        }

        @Override
        public EncryptedPayload toEncryptedEventPayload() {
            return new EncryptedPayload(eventPayload);
        }

        @Override
        public OwnedBy toOwnedBy() {
            return new OwnedBy(ownedBy);
        }
    }

    @Mock
    TargetEventChannelExecutor<Todo> targetEventChannelExecutor;

    @Mock
    IdempotencyRepository idempotencyRepository;

    @InjectMocks
    TodoTargetEventChannelConsumer todoTargetEventChannelConsumer;

    @Test
    void shouldExecuteWhenNotConsumedYetOnFirstEvent() {
        // Given
        final Target givenTarget = new Target("statistics");
        final ApplicationNaming givenApplicationNaming = new ApplicationNaming("TodoTaking", "Todo");
        final EventKey givenEventKey = TodoEventKey.of();
        final EventRecord givenEventRecord = TodoEventRecord.of();
        doReturn(Optional.empty()).when(idempotencyRepository).findLastAggregateVersionBy(
                givenTarget, givenApplicationNaming, givenEventKey.toAggregateRootType(), givenEventKey.toAggregateId());

        // When
        todoTargetEventChannelConsumer.handleMessage(givenTarget, givenApplicationNaming, givenEventKey, givenEventRecord);

        // Then
        assertAll(
                () -> verify(targetEventChannelExecutor, times(1)).execute(
                        givenTarget, givenApplicationNaming, givenEventKey, givenEventRecord),
                () -> verify(targetEventChannelExecutor, times(0)).execute(
                        any(Target.class), any(ApplicationNaming.class), any(EventKey.class), any(EventRecord.class), any(LastConsumedAggregateVersion.class)),
                () -> verify(idempotencyRepository, times(1)).upsert(
                        givenTarget, givenApplicationNaming, givenEventKey),
                () -> verify(targetEventChannelExecutor, times(0)).onAlreadyConsumed(
                        any(Target.class), any(ApplicationNaming.class), any(EventKey.class), any(EventRecord.class))
        );
    }

    @Test
    void shouldExecuteWhenNotConsumedYetOnNextEvent() {
        // Given
        final Target givenTarget = new Target("statistics");
        final ApplicationNaming givenApplicationNaming = new ApplicationNaming("TodoTaking", "Todo");
        final EventKey givenEventKey = TodoEventKey.of();
        final EventRecord givenEventRecord = TodoEventRecord.of();
        doReturn(Optional.of(new LastConsumedAggregateVersion(0))).when(idempotencyRepository).findLastAggregateVersionBy(
                givenTarget, givenApplicationNaming, givenEventKey.toAggregateRootType(), givenEventKey.toAggregateId());

        // When
        todoTargetEventChannelConsumer.handleMessage(givenTarget, givenApplicationNaming, givenEventKey, givenEventRecord);

        // Then
        assertAll(
                () -> verify(targetEventChannelExecutor, times(0)).execute(
                        any(Target.class), any(ApplicationNaming.class), any(EventKey.class), any(EventRecord.class)),
                () -> verify(targetEventChannelExecutor, times(1)).execute(
                        givenTarget, givenApplicationNaming, givenEventKey, givenEventRecord, new LastConsumedAggregateVersion(0)),
                () -> verify(idempotencyRepository, times(1)).upsert(
                        givenTarget, givenApplicationNaming, givenEventKey),
                () -> verify(targetEventChannelExecutor, times(0)).onAlreadyConsumed(
                        any(Target.class), any(ApplicationNaming.class), any(EventKey.class), any(EventRecord.class))
        );
    }

    @Test
    void shouldNotConsumeWhenAlreadyConsumed() {
        // Given
        final Target givenTarget = new Target("statistics");
        final ApplicationNaming givenApplicationNaming = new ApplicationNaming("TodoTaking", "Todo");
        final EventKey givenEventKey = TodoEventKey.of();
        final EventRecord givenEventRecord = TodoEventRecord.of();
        doReturn(Optional.of(new LastConsumedAggregateVersion(1))).when(idempotencyRepository).findLastAggregateVersionBy(
                givenTarget, givenApplicationNaming, givenEventKey.toAggregateRootType(), givenEventKey.toAggregateId());

        // When
        todoTargetEventChannelConsumer.handleMessage(givenTarget, givenApplicationNaming, givenEventKey, givenEventRecord);

        // Then
        assertAll(
                () -> verify(targetEventChannelExecutor, times(0)).execute(
                        any(Target.class), any(ApplicationNaming.class), any(EventKey.class), any(EventRecord.class)),
                () -> verify(targetEventChannelExecutor, times(0)).execute(
                        any(Target.class), any(ApplicationNaming.class), any(EventKey.class), any(EventRecord.class), any(LastConsumedAggregateVersion.class)),
                () -> verify(idempotencyRepository, times(0)).upsert(
                        any(Target.class), any(ApplicationNaming.class), any(EventKey.class)),
                () -> verify(targetEventChannelExecutor, times(1)).onAlreadyConsumed(
                        givenTarget, givenApplicationNaming, givenEventKey, givenEventRecord)
        );
    }
}

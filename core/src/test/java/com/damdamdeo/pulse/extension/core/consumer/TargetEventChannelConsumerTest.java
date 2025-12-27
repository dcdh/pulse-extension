package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyKey;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyRepository;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByDecoder;
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
            return new TodoEventKey(Todo.class.getSimpleName(), new TodoId("Damien", 0L).toString(), 1);
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

    record TodoEventValue(Long createDate,
                          String eventType,
                          byte[] eventPayload,
                          String ownedBy,
                          String belongsTo,
                          String executedBy) implements EventValue {

        public static TodoEventValue of() {
            return new TodoEventValue(
                    1983L, NewTodoCreated.class.getSimpleName(), "eventPayload".getBytes(StandardCharsets.UTF_8),
                    "Damien", "Damien/0L", "EU:bob");
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

        @Override
        public ExecutedBy toExecutedBy(final ExecutedByDecoder executedByDecoder) {
            return ExecutedBy.decode(executedBy, executedByDecoder);
        }

        @Override
        public BelongsTo toBelongsTo() {
            return new BelongsTo(new AnyAggregateId(belongsTo));
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
        final FromApplication givenFromApplication = new FromApplication("TodoTaking", "Todo");
        final EventKey givenEventKey = TodoEventKey.of();
        final EventValue givenEventValue = TodoEventValue.of();
        doReturn(Optional.empty()).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenTarget, givenFromApplication, givenEventKey.toAggregateRootType(), givenEventKey.toAggregateId()));

        // When
        todoTargetEventChannelConsumer.handleMessage(givenTarget, givenFromApplication, givenEventKey, givenEventValue);

        // Then
        assertAll(
                () -> verify(targetEventChannelExecutor, times(1)).execute(
                        givenTarget, givenFromApplication, givenEventKey, givenEventValue),
                () -> verify(targetEventChannelExecutor, times(0)).execute(
                        any(Target.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class), any(LastConsumedAggregateVersion.class)),
                () -> verify(idempotencyRepository, times(1)).upsert(
                        new IdempotencyKey(givenTarget, givenFromApplication, givenEventKey.toAggregateRootType(),
                                givenEventKey.toAggregateId()), givenEventKey.toCurrentVersionInConsumption()),
                () -> verify(targetEventChannelExecutor, times(0)).onAlreadyConsumed(
                        any(Target.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class))
        );
    }

    @Test
    void shouldExecuteWhenNotConsumedYetOnNextEvent() {
        // Given
        final Target givenTarget = new Target("statistics");
        final FromApplication givenFromApplication = new FromApplication("TodoTaking", "Todo");
        final EventKey givenEventKey = TodoEventKey.of();
        final EventValue givenEventValue = TodoEventValue.of();
        doReturn(Optional.of(new LastConsumedAggregateVersion(0))).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenTarget, givenFromApplication, givenEventKey.toAggregateRootType(), givenEventKey.toAggregateId()));

        // When
        todoTargetEventChannelConsumer.handleMessage(givenTarget, givenFromApplication, givenEventKey, givenEventValue);

        // Then
        assertAll(
                () -> verify(targetEventChannelExecutor, times(0)).execute(
                        any(Target.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class)),
                () -> verify(targetEventChannelExecutor, times(1)).execute(
                        givenTarget, givenFromApplication, givenEventKey, givenEventValue, new LastConsumedAggregateVersion(0)),
                () -> verify(idempotencyRepository, times(1)).upsert(
                        new IdempotencyKey(givenTarget, givenFromApplication, givenEventKey.toAggregateRootType(),
                                givenEventKey.toAggregateId()), givenEventKey.toCurrentVersionInConsumption()),
                () -> verify(targetEventChannelExecutor, times(0)).onAlreadyConsumed(
                        any(Target.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class))
        );
    }

    @Test
    void shouldNotConsumeWhenAlreadyConsumed() {
        // Given
        final Target givenTarget = new Target("statistics");
        final FromApplication givenFromApplication = new FromApplication("TodoTaking", "Todo");
        final EventKey givenEventKey = TodoEventKey.of();
        final EventValue givenEventValue = TodoEventValue.of();
        doReturn(Optional.of(new LastConsumedAggregateVersion(1))).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenTarget, givenFromApplication, givenEventKey.toAggregateRootType(),
                        givenEventKey.toAggregateId()));

        // When
        todoTargetEventChannelConsumer.handleMessage(givenTarget, givenFromApplication, givenEventKey, givenEventValue);

        // Then
        assertAll(
                () -> verify(targetEventChannelExecutor, times(0)).execute(
                        any(Target.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class)),
                () -> verify(targetEventChannelExecutor, times(0)).execute(
                        any(Target.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class), any(LastConsumedAggregateVersion.class)),
                () -> verify(idempotencyRepository, times(0)).upsert(any(IdempotencyKey.class), any(CurrentVersionInConsumption.class)),
                () -> verify(targetEventChannelExecutor, times(1)).onAlreadyConsumed(
                        givenTarget, givenFromApplication, givenEventKey, givenEventValue)
        );
    }
}

package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyKey;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyRepository;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.Topic;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TargetAggregateRootChannelConsumerTest {

    private static class TodoTargetAggregateRootChannelConsumer extends AbstractTargetAggregateRootChannelConsumer<Todo> {

        public TodoTargetAggregateRootChannelConsumer(final TargetAggregateRootChannelExecutor<Todo> targetAggregateRootChannelExecutor,
                                                      final IdempotencyRepository idempotencyRepository) {
            super(targetAggregateRootChannelExecutor, idempotencyRepository);
        }

    }

    record TodoAggregateRootKey(String aggregateRootType,
                                String aggregateRootId,
                                Integer version) implements AggregateRootKey {

        public static TodoAggregateRootKey of() {
            return new TodoAggregateRootKey(Todo.class.getSimpleName(), new TodoId("Damien", 0L).toString(), 1);
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

    record TodoAggregateRootValue(byte[] payload,
                                  String ownedBy,
                                  String belongsTo) implements AggregateRootValue {

        public static TodoAggregateRootValue of() {
            return new TodoAggregateRootValue("aggregateRootPayload".getBytes(StandardCharsets.UTF_8),
                    "Damien", "Damien/0L");
        }

        @Override
        public EncryptedPayload toEncryptedPayload() {
            return new EncryptedPayload(payload);
        }

        @Override
        public OwnedBy toOwnedBy() {
            return new OwnedBy(ownedBy);
        }

        @Override
        public BelongsTo toBelongsTo() {
            return new BelongsTo(new AnyAggregateId(belongsTo));
        }
    }

    @Mock
    TargetAggregateRootChannelExecutor<Todo> targetAggregateRootChannelExecutor;

    @Mock
    IdempotencyRepository idempotencyRepository;

    @InjectMocks
    TodoTargetAggregateRootChannelConsumer todoTargetAggregateRootChannelConsumer;

    @Test
    void shouldExecuteWhenNotConsumedYetOnFirstEvent() {
        // Given
        final Target givenTarget = new Target("statistics");
        final FromApplication givenFromApplication = new FromApplication("TodoTaking", "Todo");
        final AggregateRootKey givenAggregateRootKey = TodoAggregateRootKey.of();
        final AggregateRootValue givenTodoAggregateRootValue = TodoAggregateRootValue.of();
        doReturn(Optional.empty()).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenTarget, givenFromApplication, Topic.AGGREGATE_ROOT, givenAggregateRootKey.toAggregateRootType(), givenAggregateRootKey.toAggregateId()));

        // When
        todoTargetAggregateRootChannelConsumer.handleMessage(givenTarget, givenFromApplication, givenAggregateRootKey,
                givenTodoAggregateRootValue);

        // Then
        assertAll(
                () -> verify(targetAggregateRootChannelExecutor, times(1)).execute(
                        givenTarget, givenFromApplication, givenAggregateRootKey, givenTodoAggregateRootValue),
                () -> verify(targetAggregateRootChannelExecutor, times(0)).execute(
                        any(Target.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class), any(LastConsumedAggregateVersion.class)),
                () -> verify(idempotencyRepository, times(1)).upsert(
                        new IdempotencyKey(givenTarget, givenFromApplication, Topic.AGGREGATE_ROOT, givenAggregateRootKey.toAggregateRootType(),
                                givenAggregateRootKey.toAggregateId()), givenAggregateRootKey.toCurrentVersionInConsumption()),
                () -> verify(targetAggregateRootChannelExecutor, times(0)).onAlreadyConsumed(
                        any(Target.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class))
        );
    }

    @Test
    void shouldExecuteWhenNotConsumedYetOnNextEvent() {
        // Given
        final Target givenTarget = new Target("statistics");
        final FromApplication givenFromApplication = new FromApplication("TodoTaking", "Todo");
        final AggregateRootKey givenAggregateRootKey = TodoAggregateRootKey.of();
        final AggregateRootValue givenTodoAggregateRootValue = TodoAggregateRootValue.of();
        doReturn(Optional.of(new LastConsumedAggregateVersion(0))).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenTarget, givenFromApplication, Topic.AGGREGATE_ROOT, givenAggregateRootKey.toAggregateRootType(), givenAggregateRootKey.toAggregateId()));

        // When
        todoTargetAggregateRootChannelConsumer.handleMessage(givenTarget, givenFromApplication, givenAggregateRootKey, givenTodoAggregateRootValue);

        // Then
        assertAll(
                () -> verify(targetAggregateRootChannelExecutor, times(0)).execute(
                        any(Target.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class)),
                () -> verify(targetAggregateRootChannelExecutor, times(1)).execute(
                        givenTarget, givenFromApplication, givenAggregateRootKey, givenTodoAggregateRootValue, new LastConsumedAggregateVersion(0)),
                () -> verify(idempotencyRepository, times(1)).upsert(
                        new IdempotencyKey(givenTarget, givenFromApplication, Topic.AGGREGATE_ROOT, givenAggregateRootKey.toAggregateRootType(),
                                givenAggregateRootKey.toAggregateId()), givenAggregateRootKey.toCurrentVersionInConsumption()),
                () -> verify(targetAggregateRootChannelExecutor, times(0)).onAlreadyConsumed(
                        any(Target.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class))
        );
    }

    @Test
    void shouldNotConsumeWhenAlreadyConsumed() {
        // Given
        final Target givenTarget = new Target("statistics");
        final FromApplication givenFromApplication = new FromApplication("TodoTaking", "Todo");
        final AggregateRootKey givenAggregateRootKey = TodoAggregateRootKey.of();
        final AggregateRootValue givenTodoAggregateRootValue = TodoAggregateRootValue.of();
        doReturn(Optional.of(new LastConsumedAggregateVersion(1))).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenTarget, givenFromApplication, Topic.AGGREGATE_ROOT, givenAggregateRootKey.toAggregateRootType(),
                        givenAggregateRootKey.toAggregateId()));

        // When
        todoTargetAggregateRootChannelConsumer.handleMessage(givenTarget, givenFromApplication, givenAggregateRootKey,
                givenTodoAggregateRootValue);

        // Then
        assertAll(
                () -> verify(targetAggregateRootChannelExecutor, times(0)).execute(
                        any(Target.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class)),
                () -> verify(targetAggregateRootChannelExecutor, times(0)).execute(
                        any(Target.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class), any(LastConsumedAggregateVersion.class)),
                () -> verify(idempotencyRepository, times(0)).upsert(any(IdempotencyKey.class), any(CurrentVersionInConsumption.class)),
                () -> verify(targetAggregateRootChannelExecutor, times(1)).onAlreadyConsumed(
                        givenTarget, givenFromApplication, givenAggregateRootKey, givenTodoAggregateRootValue)
        );
    }
}

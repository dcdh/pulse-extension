package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyKey;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyRepository;
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
class PurposeAggregateRootChannelConsumerTest {

    private static class TodoPurposeAggregateRootChannelConsumer extends AbstractPurposeAggregateRootChannelConsumer<Todo> {

        public TodoPurposeAggregateRootChannelConsumer(final PurposeAggregateRootChannelExecutor<Todo> purposeAggregateRootChannelExecutor,
                                                       final IdempotencyRepository idempotencyRepository) {
            super(purposeAggregateRootChannelExecutor, idempotencyRepository);
        }

    }

    record TodoAggregateRootKey(String aggregateRootType,
                                String aggregateRootId,
                                Integer version) implements AggregateRootKey {

        public static TodoAggregateRootKey of() {
            return new TodoAggregateRootKey(Todo.class.getSimpleName(), new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1).toString(), 1);
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
                    UserId.USER_1.id(), TodoId.USER_1_TODO_1.id());
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
            return BelongsTo.from(new AnyAggregateId(belongsTo));
        }
    }

    @Mock
    PurposeAggregateRootChannelExecutor<Todo> purposeAggregateRootChannelExecutor;

    @Mock
    IdempotencyRepository idempotencyRepository;

    @InjectMocks
    TodoPurposeAggregateRootChannelConsumer todoTargetAggregateRootChannelConsumer;

    @Test
    void shouldExecuteWhenNotConsumedYetOnFirstEvent() throws UnableToExecuteException {
        // Given
        final Purpose givenPurpose = new Purpose("statistics");
        final FromApplication givenFromApplication = new FromApplication(new ApplicationNaming("TodoTaking"));
        final AggregateRootKey givenAggregateRootKey = TodoAggregateRootKey.of();
        final AggregateRootValue givenTodoAggregateRootValue = TodoAggregateRootValue.of();
        doReturn(Optional.empty()).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenPurpose, givenFromApplication, Table.AGGREGATE_ROOT, givenAggregateRootKey.toAggregateRootType(), givenAggregateRootKey.toAggregateId()));

        // When
        todoTargetAggregateRootChannelConsumer.handleMessage(givenPurpose, givenFromApplication, givenAggregateRootKey,
                givenTodoAggregateRootValue);

        // Then
        assertAll(
                () -> verify(purposeAggregateRootChannelExecutor, times(1)).execute(
                        givenPurpose, givenFromApplication, givenAggregateRootKey, givenTodoAggregateRootValue),
                () -> verify(purposeAggregateRootChannelExecutor, times(0)).execute(
                        any(Purpose.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class), any(LastConsumedAggregateVersion.class)),
                () -> verify(idempotencyRepository, times(1)).upsert(
                        new IdempotencyKey(givenPurpose, givenFromApplication, Table.AGGREGATE_ROOT, givenAggregateRootKey.toAggregateRootType(),
                                givenAggregateRootKey.toAggregateId()), givenAggregateRootKey.toCurrentVersionInConsumption()),
                () -> verify(purposeAggregateRootChannelExecutor, times(0)).onAlreadyConsumed(
                        any(Purpose.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class))
        );
    }

    @Test
    void shouldExecuteWhenNotConsumedYetOnNextEvent() throws UnableToExecuteException {
        // Given
        final Purpose givenPurpose = new Purpose("statistics");
        final FromApplication givenFromApplication = new FromApplication(new ApplicationNaming("TodoTaking"));
        final AggregateRootKey givenAggregateRootKey = TodoAggregateRootKey.of();
        final AggregateRootValue givenTodoAggregateRootValue = TodoAggregateRootValue.of();
        doReturn(Optional.of(new LastConsumedAggregateVersion(0))).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenPurpose, givenFromApplication, Table.AGGREGATE_ROOT, givenAggregateRootKey.toAggregateRootType(), givenAggregateRootKey.toAggregateId()));

        // When
        todoTargetAggregateRootChannelConsumer.handleMessage(givenPurpose, givenFromApplication, givenAggregateRootKey, givenTodoAggregateRootValue);

        // Then
        assertAll(
                () -> verify(purposeAggregateRootChannelExecutor, times(0)).execute(
                        any(Purpose.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class)),
                () -> verify(purposeAggregateRootChannelExecutor, times(1)).execute(
                        givenPurpose, givenFromApplication, givenAggregateRootKey, givenTodoAggregateRootValue, new LastConsumedAggregateVersion(0)),
                () -> verify(idempotencyRepository, times(1)).upsert(
                        new IdempotencyKey(givenPurpose, givenFromApplication, Table.AGGREGATE_ROOT, givenAggregateRootKey.toAggregateRootType(),
                                givenAggregateRootKey.toAggregateId()), givenAggregateRootKey.toCurrentVersionInConsumption()),
                () -> verify(purposeAggregateRootChannelExecutor, times(0)).onAlreadyConsumed(
                        any(Purpose.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class))
        );
    }

    @Test
    void shouldNotConsumeWhenAlreadyConsumed() throws UnableToExecuteException {
        // Given
        final Purpose givenPurpose = new Purpose("statistics");
        final FromApplication givenFromApplication = new FromApplication(new ApplicationNaming("TodoTaking"));
        final AggregateRootKey givenAggregateRootKey = TodoAggregateRootKey.of();
        final AggregateRootValue givenTodoAggregateRootValue = TodoAggregateRootValue.of();
        doReturn(Optional.of(new LastConsumedAggregateVersion(1))).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenPurpose, givenFromApplication, Table.AGGREGATE_ROOT, givenAggregateRootKey.toAggregateRootType(),
                        givenAggregateRootKey.toAggregateId()));

        // When
        todoTargetAggregateRootChannelConsumer.handleMessage(givenPurpose, givenFromApplication, givenAggregateRootKey,
                givenTodoAggregateRootValue);

        // Then
        assertAll(
                () -> verify(purposeAggregateRootChannelExecutor, times(0)).execute(
                        any(Purpose.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class)),
                () -> verify(purposeAggregateRootChannelExecutor, times(0)).execute(
                        any(Purpose.class), any(FromApplication.class), any(AggregateRootKey.class), any(AggregateRootValue.class), any(LastConsumedAggregateVersion.class)),
                () -> verify(idempotencyRepository, times(0)).upsert(any(IdempotencyKey.class), any(CurrentVersionInConsumption.class)),
                () -> verify(purposeAggregateRootChannelExecutor, times(1)).onAlreadyConsumed(
                        givenPurpose, givenFromApplication, givenAggregateRootKey, givenTodoAggregateRootValue)
        );
    }
}

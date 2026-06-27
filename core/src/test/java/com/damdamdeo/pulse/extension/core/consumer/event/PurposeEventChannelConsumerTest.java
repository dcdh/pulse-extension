package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.UnableToExecuteException;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyKey;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyRepository;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByFactory;
import com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurposeEventChannelConsumerTest {

    private static class TodoPurposeEventChannelConsumer extends AbstractPurposeEventChannelConsumer<Todo> {

        public TodoPurposeEventChannelConsumer(final PurposeEventChannelExecutor<Todo> purposeEventChannelExecutor,
                                               final IdempotencyRepository idempotencyRepository) {
            super(purposeEventChannelExecutor, idempotencyRepository);
        }

    }

    record TodoEventKey(String aggregateRootType,
                        String aggregateRootId,
                        Integer version) implements EventKey {

        public static TodoEventKey of() {
            return new TodoEventKey(Todo.class.getSimpleName(), new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1).toString(), 1);
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

    record TodoEventValue(ZonedDateTime storedAt,
                          String eventType,
                          byte[] eventPayload,
                          String ownedBy,
                          String belongsTo,
                          String executedBy) implements EventValue {

        public static TodoEventValue of() {
            return new TodoEventValue(
                    ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12), LocalTime.of(13, 46, 40), ZoneOffset.UTC),
                    NewTodoCreated.class.getSimpleName(), "eventPayload".getBytes(StandardCharsets.UTF_8),
                    UserId.USER_1.id(), TodoId.USER_1_TODO_1.id(), "EU:bob");
        }

        @Override
        public ZonedDateTime toStoredAt() {
            return storedAt;
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
        public ExecutedBy toExecutedBy(final ExecutedByFactory executedByFactory) throws UnableToDecodeException {
            return executedByFactory.from(executedBy, new OwnedBy(ownedBy));
        }

        @Override
        public BelongsTo toBelongsTo() {
            return BelongsTo.from(new AnyAggregateId(belongsTo));
        }
    }

    @Mock
    PurposeEventChannelExecutor<Todo> purposeEventChannelExecutor;

    @Mock
    IdempotencyRepository idempotencyRepository;

    @InjectMocks
    TodoPurposeEventChannelConsumer todoTargetEventChannelConsumer;

    @Test
    void shouldExecuteWhenNotConsumedYetOnFirstEvent() throws UnableToExecuteException {
        // Given
        final Purpose givenPurpose = new Purpose("statistics");
        final FromApplication givenFromApplication = new FromApplication(new ApplicationNaming("TodoTaking"));
        final EventKey givenEventKey = TodoEventKey.of();
        final EventValue givenEventValue = TodoEventValue.of();
        doReturn(Optional.empty()).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenPurpose, givenFromApplication, Table.EVENT, givenEventKey.toAggregateRootType(), givenEventKey.toAggregateId()));

        // When
        todoTargetEventChannelConsumer.handleMessage(givenPurpose, givenFromApplication, givenEventKey, givenEventValue);

        // Then
        assertAll(
                () -> verify(purposeEventChannelExecutor, times(1)).execute(
                        givenPurpose, givenFromApplication, givenEventKey, givenEventValue),
                () -> verify(purposeEventChannelExecutor, times(0)).execute(
                        any(Purpose.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class), any(LastConsumedAggregateVersion.class)),
                () -> verify(idempotencyRepository, times(1)).upsert(
                        new IdempotencyKey(givenPurpose, givenFromApplication, Table.EVENT, givenEventKey.toAggregateRootType(),
                                givenEventKey.toAggregateId()), givenEventKey.toCurrentVersionInConsumption()),
                () -> verify(purposeEventChannelExecutor, times(0)).onAlreadyConsumed(
                        any(Purpose.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class))
        );
    }

    @Test
    void shouldExecuteWhenNotConsumedYetOnNextEvent() throws UnableToExecuteException {
        // Given
        final Purpose givenPurpose = new Purpose("statistics");
        final FromApplication givenFromApplication = new FromApplication(new ApplicationNaming("TodoTaking"));
        final EventKey givenEventKey = TodoEventKey.of();
        final EventValue givenEventValue = TodoEventValue.of();
        doReturn(Optional.of(new LastConsumedAggregateVersion(0))).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenPurpose, givenFromApplication, Table.EVENT, givenEventKey.toAggregateRootType(), givenEventKey.toAggregateId()));

        // When
        todoTargetEventChannelConsumer.handleMessage(givenPurpose, givenFromApplication, givenEventKey, givenEventValue);

        // Then
        assertAll(
                () -> verify(purposeEventChannelExecutor, times(0)).execute(
                        any(Purpose.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class)),
                () -> verify(purposeEventChannelExecutor, times(1)).execute(
                        givenPurpose, givenFromApplication, givenEventKey, givenEventValue, new LastConsumedAggregateVersion(0)),
                () -> verify(idempotencyRepository, times(1)).upsert(
                        new IdempotencyKey(givenPurpose, givenFromApplication, Table.EVENT, givenEventKey.toAggregateRootType(),
                                givenEventKey.toAggregateId()), givenEventKey.toCurrentVersionInConsumption()),
                () -> verify(purposeEventChannelExecutor, times(0)).onAlreadyConsumed(
                        any(Purpose.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class))
        );
    }

    @Test
    void shouldNotConsumeWhenAlreadyConsumed() throws UnableToExecuteException {
        // Given
        final Purpose givenPurpose = new Purpose("statistics");
        final FromApplication givenFromApplication = new FromApplication(new ApplicationNaming("TodoTaking"));
        final EventKey givenEventKey = TodoEventKey.of();
        final EventValue givenEventValue = TodoEventValue.of();
        doReturn(Optional.of(new LastConsumedAggregateVersion(1))).when(idempotencyRepository).findLastAggregateVersionBy(
                new IdempotencyKey(
                        givenPurpose, givenFromApplication, Table.EVENT, givenEventKey.toAggregateRootType(),
                        givenEventKey.toAggregateId()));

        // When
        todoTargetEventChannelConsumer.handleMessage(givenPurpose, givenFromApplication, givenEventKey, givenEventValue);

        // Then
        assertAll(
                () -> verify(purposeEventChannelExecutor, times(0)).execute(
                        any(Purpose.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class)),
                () -> verify(purposeEventChannelExecutor, times(0)).execute(
                        any(Purpose.class), any(FromApplication.class), any(EventKey.class), any(EventValue.class), any(LastConsumedAggregateVersion.class)),
                () -> verify(idempotencyRepository, times(0)).upsert(any(IdempotencyKey.class), any(CurrentVersionInConsumption.class)),
                () -> verify(purposeEventChannelExecutor, times(1)).onAlreadyConsumed(
                        givenPurpose, givenFromApplication, givenEventKey, givenEventValue)
        );
    }
}

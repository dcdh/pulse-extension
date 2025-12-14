package com.damdamdeo.pulse.extension.consumer.deployment.notification;

import com.damdamdeo.pulse.extension.consumer.runtime.notification.AbstractNotifierListener;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.AggregateRootLoaded;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.DecryptablePayload;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

class NotificationTest {

    putain c'est chiant !


    static public class TodoEventNotifierListener extends AbstractNotifierListener<TodoEventMessageNotificationDTO> {

        @Override
        protected boolean filter(final FromApplication fromApplication,
                                 final AggregateRootType aggregateRootType,
                                 final AggregateId aggregateId,
                                 final CurrentVersionInConsumption currentVersionInConsumption,
                                 final Instant creationDate,
                                 final EventType eventType,
                                 final EncryptedPayload encryptedPayload,
                                 final OwnedBy ownedBy,
                                 final DecryptablePayload<JsonNode> decryptableEventPayload,
                                 final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
            return true;
        }

        @Override
        protected TodoEventMessageNotificationDTO mapTo(final FromApplication fromApplication,
                                                        final AggregateRootType aggregateRootType,
                                                        final AggregateId aggregateId,
                                                        final CurrentVersionInConsumption currentVersionInConsumption,
                                                        final Instant creationDate,
                                                        final EventType eventType,
                                                        final EncryptedPayload encryptedPayload,
                                                        final OwnedBy ownedBy,
                                                        final DecryptablePayload<JsonNode> decryptableEventPayload,
                                                        final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
            if () {

            } else if () {

            }
            return ;
        }

        @Override
        protected String eventName() {
            return "TodoEvent";
        }
    }

    public record TodoEventMessageNotificationDTO(String message) {

        public TodoEventMessageNotificationDTO {
            Objects.requireNonNull(message);
        }
    }
}

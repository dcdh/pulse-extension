package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer;

import com.damdamdeo.pulse.extension.livenotifier.runtime.MessagingLiveNotifierPublisher;
import io.quarkus.arc.Unremovable;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
@Unremovable
public class MessagingConsumer {

    @Inject
    Event<NotifyEvent> notifyEventProducer;

    @Transactional
    @Blocking
    @Incoming("live-notification-in")
    public void consume(final ConsumerRecord<Void, Object> consumerRecord) {
        final String eventName = new String(consumerRecord.headers()
                .lastHeader(MessagingLiveNotifierPublisher.EVENT_NAME).value());
        // TODO
        notifyEventProducer.fire(new NotifyEvent(eventName, consumerRecord.value(), null));
    }
}

package com.damdamdeo.pulse.extension.writer.runtime;

import com.damdamdeo.pulse.extension.core.event.EventNotifier;
import com.damdamdeo.pulse.extension.core.event.IdentifiableEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.util.Objects;

@ApplicationScoped
public class JakartaEventNotifier implements EventNotifier {

    @Inject
    Event<IdentifiableEvent> notifiableEventProducer;

    @Override
    public void notify(final IdentifiableEvent identifiableEvent) {
        Objects.requireNonNull(identifiableEvent);
        notifiableEventProducer.fire(identifiableEvent);
    }
}

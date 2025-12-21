package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier;

import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.NotifyEvent;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/notifier/sse")
@ApplicationScoped
public class SseBroadcasterEndpoint {

    @Context
    Sse sse;

    SseBroadcaster sseBroadcaster;

    @PostConstruct
    void init() {
        sseBroadcaster = sse.newBroadcaster();
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void register(@Context SseEventSink eventSink) {
        sseBroadcaster.register(eventSink);
    }

    public void on(@Observes NotifyEvent notifyEvent) {
        final OutboundSseEvent event = sse.newEventBuilder()
                .name(notifyEvent.eventName())
                .data(notifyEvent.type(), notifyEvent.data())
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .build();

        Log.infov("Broadcasting ''{0}''", notifyEvent.eventName());
        sseBroadcaster.broadcast(event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        Log.warnv("Error broadcasting ''{0}'': {1}", notifyEvent.eventName(), throwable.getMessage());
                    } else {
                        Log.infov("Broadcast completed for event ''{0}''", notifyEvent.eventName());
                    }
                });
    }
}

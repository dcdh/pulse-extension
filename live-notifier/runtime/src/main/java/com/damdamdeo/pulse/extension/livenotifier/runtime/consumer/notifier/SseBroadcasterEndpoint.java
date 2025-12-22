package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier;

import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.NotifyEvent;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Path("/notifier/sse")
@ApplicationScoped
public class SseBroadcasterEndpoint {

    @Context
    Sse sse;

    private static final Map<Client, SseBroadcaster> SSE_BROADCASTERS_FOR_UNKNOW_CLIENTS = new ConcurrentHashMap<>();

    @Inject
    ClientProvider clientProvider;

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void register(@Context SseEventSink eventSink) {
        final Client client = clientProvider.provide();
        if (client.isUnknown()) {
            final SseBroadcaster sseBroadcaster = sse.newBroadcaster();
            sseBroadcaster.onClose(sseEventSink -> {
                Log.debugv("received client disconnection {0}", sseEventSink.toString());
                final SseBroadcaster remove = SSE_BROADCASTERS_FOR_UNKNOW_CLIENTS.remove(client);
                remove.close();
            });
            sseBroadcaster.onError(((sseEventSink, throwable) -> {
                Log.debugv("received client connection error {0} {1}", sseEventSink.toString(), throwable.toString());
                final SseBroadcaster remove = SSE_BROADCASTERS_FOR_UNKNOW_CLIENTS.remove(client);
                remove.close();
            }));
            sseBroadcaster.register(eventSink);
            SSE_BROADCASTERS_FOR_UNKNOW_CLIENTS.put(client, sseBroadcaster);
        }
    }

    public void on(@Observes NotifyEvent notifyEvent) {
        final OutboundSseEvent event = sse.newEventBuilder()
                .name(notifyEvent.eventName())
                .data(notifyEvent.type(), notifyEvent.data())
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .build();

        Log.infov("Broadcasting ''{0}''", notifyEvent.eventName());
        if (notifyEvent.shouldBroadcastToUnknownClients()) {
            SSE_BROADCASTERS_FOR_UNKNOW_CLIENTS.forEach(((unknownClient, sseBroadcaster) -> {
                sseBroadcaster.broadcast(event)
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                Log.warnv("Error broadcasting ''{0}'': {1}", notifyEvent.eventName(), throwable.getMessage());
                            } else {
                                Log.infov("Broadcast completed for event ''{0}''", notifyEvent.eventName());
                            }
                        });
            }));
        } else {
            final String identifier = notifyEvent.userId();
            throw new UnsupportedOperationException("TODO");
        }
    }
}

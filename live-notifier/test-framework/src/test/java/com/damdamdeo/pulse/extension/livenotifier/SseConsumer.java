package com.damdamdeo.pulse.extension.livenotifier;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class SseConsumer {

    public CompletableFuture<List<String>> consume(final String accessToken, final Duration duration) throws InterruptedException {
        Objects.requireNonNull(duration);
        final Integer testPort = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CompletableFuture<List<String>> receivedEvents = new CompletableFuture<>();
        executor.submit(() -> {
            final List<String> events = new ArrayList<>();
            try {
                final long end = System.currentTimeMillis() + duration.toMillis();

                final URL url = new URI("http://localhost:%d/notifier/sse/stream".formatted(testPort)).toURL();
                final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Accept", "text/event-stream");
                if (accessToken != null) {
                    connection.setRequestProperty("Authorization", "Bearer %s".formatted(accessToken));
                }
                connection.setConnectTimeout(5_000);
                connection.setReadTimeout((int) duration.toMillis());
                connection.connect();

                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

                    String line;
                    StringBuilder event = new StringBuilder();

                    while (System.currentTimeMillis() < end &&
                            (line = reader.readLine()) != null) {// Warning readLine can block indefinitely - Have to specify setReadTimeout

                        if (line.isBlank()) {
                            events.add(event.toString());
                            event.setLength(0); // reset for the next event
                            continue;
                        }

                        event.append(line).append("\n");
                    }
                }
                receivedEvents.complete(events);
            } catch (final IOException exception) {
                // nothing to do generally a "java.io.IOException: Premature EOF" - no matter in test
                if (!receivedEvents.isDone()) {
                    receivedEvents.complete(events);
                }
            } catch (final URISyntaxException exception) {
                throw new RuntimeException(exception);
            } finally {
                if (!receivedEvents.isDone()) {
                    receivedEvents.complete(events);
                }
                executor.shutdown();
            }
        });
        Thread.sleep(1000);
        return receivedEvents;
    }
}

package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.query.runtime.file.CachedFileRepository;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class CachedFileRepositoryPurgeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application.properties")
            .overrideRuntimeConfigKey("quarkus.cache.caffeine.\"file\".expire-after-write", "5s")
            // should not trigger the schedule task
            .overrideRuntimeConfigKey("pulse.query.file.cleanup.every", "5m");

    @ApplicationScoped
    static class TestFileInitializer {

        void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION - 100) final StartupEvent event) {
            try {
                final Path directory = CachedFileRepository.DIRECTORY;
                Files.createDirectories(directory);
                Files.deleteIfExists(directory.resolve("facture.jpg"));
                Files.createFile(directory.resolve("facture.jpg"));
            } catch (final IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
    }

    @Test
    void shouldPurgeDirectoryAtStartUp() {
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(isDirectoryEmpty(CachedFileRepository.DIRECTORY)).isTrue());
    }

    @AfterEach
    void tearDown() {
        try {
            deleteRecursively(Path.of("/tmp/pulse"));
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void deleteRecursively(final Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (final Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    private static boolean isDirectoryEmpty(final Path directory) {
        try (final Stream<Path> files = Files.list(directory)) {
            return files.findAny().isEmpty();
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

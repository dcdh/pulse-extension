package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.github.benmanes.caffeine.cache.Cache;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.interceptor.Interceptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.damdamdeo.pulse.extension.query.runtime.file.CachedFileRepository.DIRECTORY;

// https://github.com/ben-manes/caffeine/wiki/Cleanup
//        final Cleaner cleaner = Cleaner.create();
//        cleaner.register(cache, cache::cleanUp);
// nope replaced with @Scheduled
@ApplicationScoped
@Unremovable
public class FileCacheCleaner {

    @Inject
    @Named(FileCacheProducer.CACHE_NAME)
    Cache<FileIdentifier, CachedFileRepository.FileCache> cache;

    void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION) final StartupEvent startupEvent) {
        try {
            if (Files.exists(DIRECTORY)) {
                try (final Stream<Path> paths = Files.walk(DIRECTORY)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(this::delete);
                }
            }
            Files.createDirectories(DIRECTORY);
        } catch (final IOException exception) {
            throw new IllegalStateException("Unable to initialize file storage directory: " + DIRECTORY, exception);
        }
    }

    private void delete(final Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (final IOException exception) {
            throw new IllegalStateException("Unable to delete: " + path, exception);
        }
    }

    @Scheduled(every = "${pulse.query.file.cleanup.every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void cleanUp() {
        cache.cleanUp();
    }
}

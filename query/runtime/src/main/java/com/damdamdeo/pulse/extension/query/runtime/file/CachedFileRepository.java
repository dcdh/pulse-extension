package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.github.benmanes.caffeine.cache.Cache;
import io.quarkus.arc.Unremovable;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.logging.Logger;

// https://chatgpt.com/c/6a737ef4-c83c-83eb-ae0f-14205268409b

/**
 * Store encrypted files in a local directory.
 */
@Unremovable
@Priority(1)
@Decorator
public class CachedFileRepository implements FileRepository {

    static final Logger LOGGER = Logger.getLogger(CachedFileRepository.class.getName());


    // On the application Docker definition, define a --tmpfs /tmp/pulse/files
    public static final Path DIRECTORY = Path.of("/tmp/pulse/files");

    @Inject
    @Any
    @Delegate
    FileRepository delegate;

    // https://github.com/quarkusio/quarkus/blob/main/extensions/cache/runtime/src/main/java/io/quarkus/cache/runtime/caffeine/CaffeineCacheImpl.java
    // Quarkus Caffeine Cache do not allow the usage of RemovalListener
//    @Inject
//    @CacheName(CACHE_NAME)
//    Cache cache;
//    Cache<FileIdentifier, FileCache> je dois creer un nouveau encryptedFileInfo qui pointe vers un fichier à créé et c'est cool ! Il faudra tester l'eviction en attendant 10 seconds par exemple
    @Inject
    @Named(FileCacheProducer.CACHE_NAME)
    Cache<FileIdentifier, FileCache> cache;

    public record FileCache(EncryptedFileInfo encryptedFileInfo, ContentType contentType, ContentLength contentLength, Path path) {

        public FileCache {
            Objects.requireNonNull(encryptedFileInfo);
            Objects.requireNonNull(contentType);
            Objects.requireNonNull(contentLength);
            Objects.requireNonNull(path);
        }
    }

    @Override
    public boolean exists(final FileIdentifier fileIdentifier) throws FileRepositoryException {
        Objects.requireNonNull(fileIdentifier);
        if (cache.getIfPresent(fileIdentifier) != null) {
            return true;
        } else {
            return delegate.exists(fileIdentifier);
        }
    }

    @Override
    public void store(final EncryptedFileInfo encryptedFileInfo, final Encrypted<InputStream> encrypted) throws FileRepositoryException {
        Objects.requireNonNull(encryptedFileInfo);
        Objects.requireNonNull(encrypted);
        delegate.store(encryptedFileInfo, encrypted);
    }

    @Override
    public EncryptedFileInfo getFileInfoByFileIdentifier(final FileIdentifier fileIdentifier) throws FileRepositoryException {
        Objects.requireNonNull(fileIdentifier);
        if (cache.getIfPresent(fileIdentifier) != null) {
            return cache.getIfPresent(fileIdentifier).encryptedFileInfo();
        } else {
            return delegate.getFileInfoByFileIdentifier(fileIdentifier);
        }
    }

    @Override
    public FileContent getFileContentByFileIdentifier(final FileIdentifier fileIdentifier) throws FileRepositoryException {
        Objects.requireNonNull(fileIdentifier);
        try {
            if (cache.getIfPresent(fileIdentifier) != null) {
                final FileCache fileCache = cache.getIfPresent(fileIdentifier);
                assert fileCache != null;
                if (Files.exists(fileCache.path)) {
                    return new FileContent(
                            fileCache.encryptedFileInfo().fileIdentifier(),
                            fileCache.contentType(),
                            fileCache.contentLength(),
                            Files.newInputStream(fileCache.path()));
                }
            }
            final EncryptedFileInfo encryptedFileInfo = delegate.getFileInfoByFileIdentifier(fileIdentifier);
            final Path path = DIRECTORY.resolve(fileIdentifier.id() + "." + encryptedFileInfo.contentType().extension());
            Files.createFile(path);
            final FileContent fileContentByFileIdentifier = delegate.getFileContentByFileIdentifier(fileIdentifier);
            try (final InputStream in = fileContentByFileIdentifier.content()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
            final FileCache fileCache = new FileCache(encryptedFileInfo, fileContentByFileIdentifier.contentType(),
                    fileContentByFileIdentifier.contentLength(), path);
            cache.put(fileIdentifier, fileCache);
            return new FileContent(
                    fileContentByFileIdentifier.id(),
                    fileContentByFileIdentifier.contentType(),
                    fileContentByFileIdentifier.contentLength(),
                    Files.newInputStream(path));
        } catch (final IOException e) {
            LOGGER.warning("Unable to create temp file for file: " + fileIdentifier + " cause: " + e.getMessage());
            return delegate.getFileContentByFileIdentifier(fileIdentifier);
        }
    }
}

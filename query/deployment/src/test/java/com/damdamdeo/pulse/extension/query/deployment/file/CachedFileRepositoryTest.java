package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.damdamdeo.pulse.extension.query.runtime.file.CachedFileRepository;
import com.damdamdeo.pulse.extension.query.runtime.file.FileCacheProducer;
import com.github.benmanes.caffeine.cache.Cache;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

// cf CachedPassphraseRepositoryTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CachedFileRepositoryTest {

    public static ExecutedBy BOB = new ExecutedBy.EndUser("bob", true);

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application.properties")
            .overrideRuntimeConfigKey("quarkus.cache.caffeine.\"file\".expire-after-write", "5s")
            // should trigger the schedule task
            .overrideRuntimeConfigKey("pulse.query.file.cleanup.every", "5s");

    @ApplicationScoped
    static class FileRepositoryTestSpy {

        private final List<String> called = new ArrayList<>();

        public void add(String value) {
            called.add(value);
        }

        public List<String> getCalled() {
            return called;
        }

        public void reset() {
            called.clear();
        }
    }

    @Inject
    FileRepository fileRepository;

    @Inject
    FileRepositoryTestSpy fileRepositoryTestSpy;

    @Inject
    DataSource dataSource;

    @Inject
    @Named(FileCacheProducer.CACHE_NAME)
    Cache<FileIdentifier, CachedFileRepository.FileCache> cache;

    @Unremovable
    @Priority(2) // needs to be called by the CachedFileRepository or not :)
    @Decorator
    static class StubFileRepository implements FileRepository {

        @Inject
        @Any
        @Delegate
        FileRepository delegate;// Should be JdbcPostgresFileRepository

        @Inject
        FileRepositoryTestSpy fileRepositoryTestSpy;

        @Override
        public boolean exists(final FileIdentifier fileIdentifier) throws FileRepositoryException {
            Objects.requireNonNull(fileIdentifier);
            fileRepositoryTestSpy.add(String.join("|", "exists", fileIdentifier.id()));
            return delegate.exists(fileIdentifier);
        }

        @Override
        public void store(final FileInfo fileInfo, final Encrypted<InputStream> encrypted) throws FileRepositoryException {
            Objects.requireNonNull(fileInfo);
            Objects.requireNonNull(encrypted);
            fileRepositoryTestSpy.add(String.join("|", "store", fileInfo.fileIdentifier().id()));
            delegate.store(fileInfo, encrypted);
        }

        @Override
        public FileInfo getFileInfoByFileIdentifier(final FileIdentifier fileIdentifier) throws FileRepositoryException {
            Objects.requireNonNull(fileIdentifier);
            fileRepositoryTestSpy.add(String.join("|", "getFileInfoByFileIdentifier", fileIdentifier.id()));
            return delegate.getFileInfoByFileIdentifier(fileIdentifier);
        }

        @Override
        public FileContent getFileContentByFileIdentifier(final FileIdentifier fileIdentifier) throws FileRepositoryException {
            Objects.requireNonNull(fileIdentifier);
            fileRepositoryTestSpy.add(String.join("|", "getFileContentByFileIdentifier", fileIdentifier.id()));
            return delegate.getFileContentByFileIdentifier(fileIdentifier);
        }
    }

    @BeforeEach
    void setup() {
        fileRepositoryTestSpy.reset();
    }

    private FileInfo fileInfo() {
        final FileIdentifier identifier = new FileIdentifier("file-123");
        return new FileInfo(
                identifier,
                new Filename("facture.jpg"),
                ContentType.IMAGE_JPG,
                new ContentLength(13L),
                new UploadedAt(ZonedDateTime.of(LocalDate.of(2026, 8, 5), LocalTime.of(23, 0, 31), ZoneOffset.UTC)),
                new UploadedBy(BOB),
                OwnedBy.from(identifier),
                new FileMetadata(
                        Map.of(
                                "author", List.of("BOB"),
                                "tag", List.of("invoice")
                        )
                )
        );
    }

    @Test
    @Order(1)
    void shouldStoreMustNotBeCached() throws FileRepositoryException {
        // Given
        final FileInfo fileInfo = fileInfo();

        // When
        fileRepository.store(fileInfo, Encrypted.of(new ByteArrayInputStream("Encrypted !!!".getBytes(StandardCharsets.UTF_8))));

        // Then
        assertAll(
                () -> assertThat(cache.asMap().isEmpty()).isTrue(),
                () -> assertThat(fileRepositoryTestSpy.getCalled()).containsExactly("store|file-123"),
                () -> assertThat(fileIdentifiers()).containsExactly("file-123")
        );
    }

    @Test
    @Order(2)
    void shouldExistCallDelegateWhenNotInCache() throws FileRepositoryException {
        // Given
        final FileInfo fileInfo = fileInfo();

        // When
        final boolean exists = fileRepository.exists(fileInfo.fileIdentifier());

        // Then
        assertAll(
                () -> assertThat(cache.asMap().isEmpty()).isTrue(),
                () -> assertThat(exists).isTrue(),
                () -> assertThat(fileRepositoryTestSpy.getCalled()).containsExactly("exists|file-123")
        );
    }

    @Test
    @Order(3)
    void shouldGetFileInfoByFileIdentifierCallDelegateWhenNotInCache() throws FileRepositoryException {
        // Given
        final FileInfo fileInfo = fileInfo();

        // When
        final FileInfo fileInfoByFileIdentifier = fileRepository.getFileInfoByFileIdentifier(fileInfo.fileIdentifier());

        // Then
        assertAll(
                () -> assertThat(cache.asMap().isEmpty()).isTrue(),
                () -> assertThat(fileInfoByFileIdentifier.fileIdentifier()).isEqualTo(fileInfo.fileIdentifier()),
                () -> assertThat(fileRepositoryTestSpy.getCalled()).containsExactly("getFileInfoByFileIdentifier|file-123")
        );
    }

    @Test
    @Order(4)
    void shouldGetFileContentByFileIdentifierCallDelegateWhenNotInCache() throws FileRepositoryException, IOException {
        // Given
        final FileInfo fileInfo = fileInfo();

        // When
        final FileContent fileContentByFileIdentifier = fileRepository.getFileContentByFileIdentifier(fileInfo.fileIdentifier());

        // Then
        final byte[] actualContent;
        try (final InputStream content = fileContentByFileIdentifier.content()) {
            actualContent = content.readAllBytes();
        }
        assertAll(
                () -> assertThat(cache.asMap().keySet()).containsExactly(new FileIdentifier("file-123")),
                () -> assertThat(fileContentByFileIdentifier.id()).isEqualTo(fileInfo.fileIdentifier()),
                () -> assertThat(actualContent).containsExactly("Encrypted !!!".getBytes(StandardCharsets.UTF_8)),
                () -> assertThat(fileRepositoryTestSpy.getCalled()).containsExactly("getFileInfoByFileIdentifier|file-123",
                        "getFileContentByFileIdentifier|file-123")
        );
    }

    @Test
    @Order(5)
    void shouldGetFileContentByFileIdentifierReturnFromCache() throws FileRepositoryException, IOException {
        // Given
        final FileInfo fileInfo = fileInfo();

        // When
        // Use a loop to ensure that content is reopened each time it is called!
        final AtomicReference<FileContent> fileContentByFileIdentifier = new AtomicReference<>();
        final AtomicReference<byte[]> contentByFileIdentifier = new AtomicReference<>();
        for (int i = 0; i < 3; i++) {
            fileContentByFileIdentifier.set(fileRepository.getFileContentByFileIdentifier(fileInfo.fileIdentifier()));

            // Then
            try (final InputStream content = fileContentByFileIdentifier.get().content()) {
                contentByFileIdentifier.set(content.readAllBytes());
            }

        }
        assertAll(
                () -> assertThat(cache.asMap().keySet()).containsExactly(new FileIdentifier("file-123")),
                () -> assertThat(fileContentByFileIdentifier.get().id()).isEqualTo(fileInfo.fileIdentifier()),
                () -> assertThat(contentByFileIdentifier.get()).containsExactly("Encrypted !!!".getBytes(StandardCharsets.UTF_8)),
                () -> assertThat(fileRepositoryTestSpy.getCalled()).isEmpty()
        );
    }

    @Test
    @Order(6)
    void shouldExistsReturnFromCache() throws FileRepositoryException {
        // Given
        final FileInfo fileInfo = fileInfo();

        // When
        final boolean exists = fileRepository.exists(fileInfo.fileIdentifier());

        // Then
        assertAll(
                () -> assertThat(cache.asMap().keySet()).containsExactly(new FileIdentifier("file-123")),
                () -> assertThat(exists).isTrue(),
                () -> assertThat(fileRepositoryTestSpy.getCalled()).isEmpty()
        );
    }

    @Test
    @Order(7)
    void shouldGetFileInfoByFileIdentifierReturnFromCache() throws FileRepositoryException {
        // Given
        final FileInfo fileInfo = fileInfo();

        // When
        final FileInfo fileInfoByFileIdentifier = fileRepository.getFileInfoByFileIdentifier(fileInfo.fileIdentifier());

        // Then
        assertAll(
                () -> assertThat(cache.asMap().keySet()).containsExactly(new FileIdentifier("file-123")),
                () -> assertThat(fileInfoByFileIdentifier.fileIdentifier()).isEqualTo(fileInfo.fileIdentifier()),
                () -> assertThat(fileRepositoryTestSpy.getCalled()).isEmpty()
        );
    }

    @Test
    @Order(8)
    void shouldRemoveFileAtCacheRemoval() {
        // Given
        final FileInfo fileInfo = fileInfo();

        // When && Then
//        await().atMost(30, TimeUnit.SECONDS)
//                .untilAsserted(() -> assertAll(
//                        () -> assertThat(Files.exists(CachedFileRepository.DIRECTORY.resolve(fileInfo.fileIdentifier().id())))
//                                .isFalse(),
//                        () -> assertThat(cache.asMap().isEmpty()).isTrue()
//                ));
        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(Files.exists(CachedFileRepository.DIRECTORY.resolve(
                        fileInfo.fileIdentifier().id() + "." + fileInfo.contentType().extension())))
                        .isFalse());
        assertThat(cache.asMap().isEmpty()).isTrue();
    }

    @Test
    @Order(9)
    void shouldReloadCacheWhenFileReferencedInCacheDoesNotExistsAnymore() throws FileRepositoryException, IOException {
        // Given
        final FileInfo fileInfo = fileInfo();
        final FileContent fileContentByFileIdentifier = fileRepository.getFileContentByFileIdentifier(fileInfo.fileIdentifier());
        final Path cachedFile = CachedFileRepository.DIRECTORY.resolve(
                fileContentByFileIdentifier.id().id() + "." + fileContentByFileIdentifier.contentType().extension());
        Validate.validState(Files.exists(cachedFile));
        Files.delete(cachedFile);

        // When
        fileRepository.getFileContentByFileIdentifier(fileInfo.fileIdentifier());

        // Then
        final byte[] actualContent;
        try (final InputStream content = fileContentByFileIdentifier.content()) {
            actualContent = content.readAllBytes();
        }
        assertAll(
                () -> assertThat(cache.asMap().keySet()).containsExactly(new FileIdentifier("file-123")),
                () -> assertThat(fileContentByFileIdentifier.id()).isEqualTo(fileInfo.fileIdentifier()),
                () -> assertThat(actualContent).containsExactly("Encrypted !!!".getBytes(StandardCharsets.UTF_8)),
                () -> assertThat(fileRepositoryTestSpy.getCalled()).containsExactly("getFileInfoByFileIdentifier|file-123",
                        "getFileContentByFileIdentifier|file-123",
                        "getFileInfoByFileIdentifier|file-123",
                        "getFileContentByFileIdentifier|file-123")
        );
    }

    private List<String> fileIdentifiers() {
        final List<String> fileIdentifiers = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("SELECT file_identifier FROM pulse.file")) {
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    fileIdentifiers.add(resultSet.getString("file_identifier"));
                }
            }
            return fileIdentifiers;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

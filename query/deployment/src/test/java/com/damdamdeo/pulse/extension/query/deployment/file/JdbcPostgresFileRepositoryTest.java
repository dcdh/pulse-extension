package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.damdamdeo.pulse.extension.query.runtime.file.CachedFileRepository;
import com.damdamdeo.pulse.extension.query.runtime.file.JdbcPostgresFileRepository;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcPostgresFileRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResourceProvider.class, Resource.class)
                    .addAsResource("facture.jpg"))
            .overrideConfigKey("quarkus.arc.exclude-types", CachedFileRepository.class.getName())
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    JdbcPostgresFileRepository jdbcPostgresFileRepository;

    private EncryptedFileInfo encryptedFileInfo() {
        final FileIdentifier identifier = new FileIdentifier("file-123");
        return new EncryptedFileInfo(
                identifier,
                new Filename("facture.jpg"),
                ContentType.IMAGE_JPG,
                new ContentLength(287759L),
                new UploadedAt(ZonedDateTime.of(LocalDate.of(2026, 8, 5), LocalTime.of(23, 0, 31), ZoneOffset.UTC).toInstant()),
                new EncryptedUploadedBy(new ExecutedByEncoded("NA"), OwnedBy.from(identifier)),
                OwnedBy.from(identifier),
                new EncryptedFileMetadata(Encrypted.of("encryptedFileMetadata".getBytes()), OwnedBy.from(identifier)),
                new EncryptedCustomMetadata(Encrypted.of("encryptedCustomMetadata".getBytes()), OwnedBy.from(identifier))
        );
    }

    @Test
    @Order(1)
    void shouldStoreAndCheckExists() throws Exception {
        // Given
        final EncryptedFileInfo givenEncryptedFileInfo = encryptedFileInfo();
        final Resource resource = TestResourceProvider.getResourceFromStream("/facture.jpg");

        // When
        jdbcPostgresFileRepository.store(givenEncryptedFileInfo, Encrypted.of(resource.payload(), resource.size()));

        // Then
        assertThat(jdbcPostgresFileRepository.exists(givenEncryptedFileInfo.fileIdentifier())).isTrue();
    }

    @Test
    @Order(2)
    void shouldRetrieveFileInfo() throws Exception {
        // Given
        final EncryptedFileInfo givenEncryptedFileInfo = encryptedFileInfo();

        // When
        final EncryptedFileInfo result = jdbcPostgresFileRepository.getFileInfoByFileIdentifier(givenEncryptedFileInfo.fileIdentifier());

        // Then
        assertAll(
                () -> assertThat(result.fileIdentifier()).isEqualTo(givenEncryptedFileInfo.fileIdentifier()),
                () -> assertThat(result.filename().filename()).isEqualTo("facture.jpg"),
                () -> assertThat(result.contentType()).isEqualTo(ContentType.IMAGE_JPG),
                () -> assertThat(result.ownedBy()).isEqualTo(new OwnedBy("file-123")),
                () -> assertThat(result.encryptedFileMetadata().encrypted().payload()).containsExactly("encryptedFileMetadata".getBytes()),
                () -> assertThat(result.encryptedCustomMetadata().encrypted().payload()).containsExactly("encryptedCustomMetadata".getBytes())
        );
    }

    @Test
    @Order(3)
    void shouldRetrieveFileContentAsStream() throws Exception {
        // Given
        final EncryptedFileInfo givenEncryptedFileInfo = encryptedFileInfo();
        final byte[] expected;
        try (final InputStream content = getClass().getResourceAsStream("/facture.jpg")) {
            assert content != null;
            expected = content.readAllBytes();
        }

        // When
        final FileContent result = jdbcPostgresFileRepository.getFileContentByFileIdentifier(givenEncryptedFileInfo.fileIdentifier());

        // Then
        final byte[] received;
        try (final InputStream stream = result.content()) {
            received = stream.readAllBytes();
        }

        assertAll(
                () -> assertThat(result.contentLength().contentLength()).isEqualTo(expected.length),
                () -> assertThat(received).isEqualTo(expected)
        );
    }

    @Test
    @Order(4)
    void shouldFailWhenFileDoesNotExist() {
        assertThatThrownBy(() -> jdbcPostgresFileRepository.getFileInfoByFileIdentifier(new FileIdentifier("unknown")))
                .isExactlyInstanceOf(FileRepositoryException.class);
    }

    @Test
    @Order(5)
    void shouldNotInsertSameFileTwice() {
        // Given
        final EncryptedFileInfo givenEncryptedFileInfo = encryptedFileInfo();
        final Resource resource = TestResourceProvider.getResourceFromStream("/facture.jpg");

        // When && Then
        assertThatThrownBy(() -> jdbcPostgresFileRepository.store(givenEncryptedFileInfo, Encrypted.of(resource.payload(), resource.size())))
                .isExactlyInstanceOf(FileRepositoryException.class)
                .cause()
                .isExactlyInstanceOf(FileAlreadyUploadedException.class);
    }

    @Test
    @Order(6)
    void shouldPreventDeleteFile() {
        // Given
        final EncryptedFileInfo givenEncryptedFileInfo = encryptedFileInfo();

        // When && Then
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                         DELETE FROM pulse.file
                                         WHERE file_identifier = ?
                                         """)) {
                statement.setString(1, givenEncryptedFileInfo.fileIdentifier().id());
                statement.executeUpdate();
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageStartingWith("ERROR: Deletion of token_download is forbidden");
    }
}

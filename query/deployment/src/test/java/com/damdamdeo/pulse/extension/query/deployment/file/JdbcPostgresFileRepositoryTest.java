package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcPostgresFileRepositoryTest {

    public static ExecutedBy BOB = new ExecutedBy.EndUser("bob", true);

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

    private FileInfo fileInfo() {
        final FileIdentifier identifier = new FileIdentifier("file-123");
        return new FileInfo(
                identifier,
                new Filename("facture.jpg"),
                ContentType.IMAGE_JPG,
                new ContentLength(287759L),
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
    void shouldStoreAndCheckExists() throws Exception {
        // Given
        final FileInfo fileInfo = fileInfo();
        final Resource resource = TestResourceProvider.getResourceAsEncryptedStream("/facture.jpg");

        // When
        jdbcPostgresFileRepository.store(fileInfo, new Encrypted<>(resource.payload(), resource.size()));

        // Then
        assertThat(jdbcPostgresFileRepository.exists(fileInfo.fileIdentifier())).isTrue();
    }

    @Test
    @Order(2)
    void shouldRetrieveFileInfo() throws Exception {
        // Given
        final FileInfo given = fileInfo();

        // When
        final FileInfo result = jdbcPostgresFileRepository.getFileInfoByFileIdentifier(given.fileIdentifier());

        // Then
        assertAll(
                () -> assertThat(result.fileIdentifier()).isEqualTo(given.fileIdentifier()),
                () -> assertThat(result.filename().filename()).isEqualTo("facture.jpg"),
                () -> assertThat(result.contentType()).isEqualTo(ContentType.IMAGE_JPG),
                () -> assertThat(result.ownedBy()).isEqualTo(new OwnedBy("file-123")),
                () -> assertThat(result.fileMetadata().metadata().get("author")).containsExactly("BOB")
        );
    }

    @Test
    @Order(3)
    void shouldRetrieveFileContentAsStream() throws Exception {
        // Given
        final FileInfo given = fileInfo();
        final byte[] expected;
        try (final InputStream content = getClass().getResourceAsStream("/facture.jpg")) {
            assert content != null;
            expected = content.readAllBytes();
        }

        // When
        final FileContent result = jdbcPostgresFileRepository.getFileContentByFileIdentifier(given.fileIdentifier());

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
    void shouldNotInsertSameFileTwice() throws Exception {
        // Given
        final FileInfo fileInfo = fileInfo();
        final Resource resource = TestResourceProvider.getResourceAsEncryptedStream("/facture.jpg");

        // When && Then
        assertThatThrownBy(() -> jdbcPostgresFileRepository.store(fileInfo, new Encrypted<>(resource.payload(), resource.size())))
                .isExactlyInstanceOf(FileRepositoryException.class)
                .cause()
                .isExactlyInstanceOf(FileAlreadyUploadedException.class);
    }

    @Test
    @Order(6)
    void shouldPreventDeleteFile() {
        // Given
        final FileInfo fileInfo = fileInfo();

        // When && Then
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                         DELETE FROM pulse.file
                                         WHERE file_identifier = ?
                                         """)) {
                statement.setString(1, fileInfo.fileIdentifier().id());
                statement.executeUpdate();
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageStartingWith("ERROR: Deletion of token_download is forbidden");
    }
}

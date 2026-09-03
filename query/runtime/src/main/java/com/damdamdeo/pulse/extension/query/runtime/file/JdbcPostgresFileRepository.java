package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.common.runtime.encryption.FutureAwareInputStream;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.query.file.*;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
@Unremovable
public class JdbcPostgresFileRepository implements FileRepository {

    @Inject
    DataSource dataSource;

    @Override
    public boolean exists(final FileIdentifier fileIdentifier) throws FileRepositoryException {
        Objects.requireNonNull(fileIdentifier);
        final String sql = """
                SELECT 1 FROM pulse.file WHERE file_identifier = ?
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileIdentifier.id());
            try (final ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (final SQLException exception) {
            throw new FileRepositoryException(exception);
        }
    }

    @Override
    public void store(final EncryptedFileInfo encryptedFileInfo, final Encrypted<InputStream> encrypted) throws FileRepositoryException {
        Objects.requireNonNull(encryptedFileInfo);
        Objects.requireNonNull(encrypted);
        final String sql = """
                INSERT INTO pulse.file (
                    file_identifier,
                    filename,
                    content_type,
                    content_length,
                    uploaded_at,
                    uploaded_by,
                    owned_by,
                    metadata,
                    content,
                    custom_metadata                    
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (file_identifier) DO NOTHING;
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, encryptedFileInfo.fileIdentifier().id());
            statement.setString(2, encryptedFileInfo.filename().filename());
            statement.setString(3, encryptedFileInfo.contentType().contentType());
            statement.setLong(4, encryptedFileInfo.contentLength().contentLength());
            statement.setObject(5, encryptedFileInfo.uploadedAt().at().atOffset(ZoneOffset.UTC));
            statement.setString(6, encryptedFileInfo.encryptedUploadedBy().executedByEncoded().encoded());
            statement.setString(7, encryptedFileInfo.ownedBy().id());
            statement.setBytes(8, encryptedFileInfo.encryptedFileMetadata().encrypted().payload());
            statement.setBinaryStream(
                    9,
                    encrypted.payload(),
                    encrypted.size()
            );
            statement.setBytes(10, encryptedFileInfo.encryptedCustomMetadata().encrypted().payload());
            if (statement.executeUpdate() == 0) {
                throw new FileAlreadyUploadedException();
            }
        } catch (final SQLException | FileAlreadyUploadedException exception) {
            throw new FileRepositoryException(exception);
        }
    }

    @Override
    public EncryptedFileInfo getFileInfoByFileIdentifier(final FileIdentifier fileIdentifier) throws FileRepositoryException {
        Objects.requireNonNull(fileIdentifier);
        final String sql = """
                SELECT
                    filename,
                    content_type,
                    content_length,
                    uploaded_at,
                    uploaded_by,
                    owned_by,
                    metadata,
                    custom_metadata
                FROM pulse.file
                WHERE file_identifier = ?
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileIdentifier.id());
            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new FileNotFoundException();
                }
                final OwnedBy ownedBy = new OwnedBy(resultSet.getString("owned_by"));
                return new EncryptedFileInfo(
                        fileIdentifier,
                        new Filename(resultSet.getString("filename")),
                        ContentType.fromContentType(resultSet.getString("content_type")),
                        new ContentLength(resultSet.getLong("content_length")),
                        new UploadedAt(resultSet.getObject("uploaded_at", OffsetDateTime.class).toInstant()),
                        new EncryptedUploadedBy(new ExecutedByEncoded(resultSet.getString("uploaded_by")), ownedBy),
                        ownedBy,
                        new EncryptedFileMetadata(Encrypted.of(resultSet.getBytes("metadata")), ownedBy),
                        new EncryptedCustomMetadata(Encrypted.of(resultSet.getBytes("custom_metadata")), ownedBy)
                );
            }
        } catch (final SQLException | FileNotFoundException exception) {
            throw new FileRepositoryException(exception);
        }
    }

    @Override
    public FileContent getFileContentByFileIdentifier(final FileIdentifier fileIdentifier) throws FileRepositoryException {
        Objects.requireNonNull(fileIdentifier);
        final String metadataSql = """
                SELECT
                    content_type,
                    content_length
                FROM pulse.file
                WHERE file_identifier = ?
                """;
        final String contentSql = """
                SELECT content
                FROM pulse.file
                WHERE file_identifier = ?
                """;
        final ContentType contentType;
        final ContentLength contentLength;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(metadataSql)) {
            statement.setString(1, fileIdentifier.id());
            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new FileNotFoundException();
                }
                contentType = ContentType.fromContentType(resultSet.getString("content_type"));
                contentLength = new ContentLength(resultSet.getLong("content_length"));
            }
        } catch (final SQLException | FileNotFoundException exception) {
            throw new FileRepositoryException(exception);
        }
        final PipedInputStream pipedInput = new PipedInputStream(64 * 1024);
        final PipedOutputStream pipedOutput;
        try {
            pipedOutput = new PipedOutputStream(pipedInput);
        } catch (final IOException exception) {
            throw new FileRepositoryException(exception);
        }
        final CompletableFuture<Void> future = new CompletableFuture<>();
        Thread.startVirtualThread(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement statement = connection.prepareStatement(contentSql)) {
                statement.setString(1, fileIdentifier.id());
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new FileNotFoundException();
                    }
                    try (final InputStream database = resultSet.getBinaryStream("content");
                         final PipedOutputStream output = pipedOutput) {
                        database.transferTo(output);
                        future.complete(null);
                    }
                }
            } catch (final Exception exception) {
                try {
                    pipedOutput.close();
                } catch (final IOException ignored) {
                }
                future.completeExceptionally(exception);
            }
        });
        return new FileContent(
                fileIdentifier,
                contentType,
                contentLength,
                new FutureAwareInputStream(
                        pipedInput,
                        future
                )
        );
    }
}

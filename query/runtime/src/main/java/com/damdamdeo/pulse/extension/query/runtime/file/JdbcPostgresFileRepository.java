package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.common.runtime.encryption.FutureAwareInputStream;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.*;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.*;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
@Unremovable
public class JdbcPostgresFileRepository implements FileRepository {

    private static final TypeReference<Map<String, List<String>>> METADATA_TYPE = new TypeReference<>() {
    };

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ExecutedByFactory executedByFactory;

    @Inject
    ExecutedByEncoder executedByEncoder;

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
    public void store(final FileInfo fileInfo, final Encrypted<InputStream> encrypted) throws FileRepositoryException {
        Objects.requireNonNull(fileInfo);
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
                    content
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                ON CONFLICT (file_identifier) DO NOTHING;
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileInfo.fileIdentifier().id());
            statement.setString(2, fileInfo.filename().filename());
            statement.setString(3, fileInfo.contentType().contentType());
            statement.setLong(4, fileInfo.contentLength().contentLength());
            statement.setTimestamp(5, Timestamp.from(fileInfo.uploadedAt().at().toInstant()));
            statement.setString(6, fileInfo.uploadedBy().executedBy().encode(executedByEncoder, fileInfo.ownedBy()).encoded());
            statement.setString(7, fileInfo.ownedBy().id());
            statement.setString(8, objectMapper.writeValueAsString(fileInfo.fileMetadata().metadata()));
            statement.setBinaryStream(
                    9,
                    encrypted.payload(),
                    fileInfo.contentLength().contentLength()
            );
            if (statement.executeUpdate() == 0) {
                throw new FileAlreadyUploadedException();
            }
        } catch (final SQLException | IOException | UnableToEncodeException | FileAlreadyUploadedException exception) {
            throw new FileRepositoryException(exception);
        }
    }

    @Override
    public FileInfo getFileInfoByFileIdentifier(final FileIdentifier fileIdentifier) throws FileRepositoryException {
        Objects.requireNonNull(fileIdentifier);
        final String sql = """
                SELECT
                    filename,
                    content_type,
                    content_length,
                    uploaded_at,
                    uploaded_by,
                    owned_by,
                    metadata
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
                final ExecutedBy executedBy = executedByFactory.from(resultSet.getString("uploaded_by"), ownedBy);
                return new FileInfo(
                        fileIdentifier,
                        new Filename(resultSet.getString("filename")),
                        ContentType.fromContentType(resultSet.getString("content_type")),
                        new ContentLength(resultSet.getLong("content_length")),
                        new UploadedAt(
                                resultSet.getTimestamp("uploaded_at")
                                        .toInstant()
                                        .atZone(ZoneOffset.UTC)
                        ),
                        new UploadedBy(executedBy),
                        ownedBy,
                        new FileMetadata(
                                objectMapper.readValue(
                                        resultSet.getString("metadata"),
                                        METADATA_TYPE
                                )
                        )
                );
            }
        } catch (final SQLException | IOException | FileNotFoundException | UnableToDecodeException exception) {
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

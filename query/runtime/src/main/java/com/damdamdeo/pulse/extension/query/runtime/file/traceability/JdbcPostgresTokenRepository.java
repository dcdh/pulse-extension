package com.damdamdeo.pulse.extension.query.runtime.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.*;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
@Unremovable
public class JdbcPostgresTokenRepository implements TokenRepository {

    @Inject
    DataSource dataSource;

    @Override
    public void store(final Traceability traceability) throws TokenRepositoryException {
        Objects.requireNonNull(traceability);
        // language=sql
        final String sql = """
                INSERT INTO pulse.token_download (token, file_identifier, downloaded_by, downloaded_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (token) DO NOTHING;
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, traceability.token().value());
            statement.setObject(2, traceability.fileIdentifier().id());
            statement.setString(3, traceability.downloadedBy().by());
            statement.setObject(4, traceability.downloadedAt().at().toOffsetDateTime());

            final int updated = statement.executeUpdate();
            if (updated != 1) {
                throw new TokenRepositoryException(new TokenAlreadyExistsException());
            }
        } catch (final SQLException exception) {
            throw new TokenRepositoryException(exception);
        }
    }

    @Override
    public List<Traceability> listByFileIdentifierOrderByDownloadedAtAsc(final FileIdentifier fileIdentifier)
            throws TokenRepositoryException {
        Objects.requireNonNull(fileIdentifier);
        // language=sql
        final String sql = """
                SELECT
                    token,
                    file_identifier,
                    downloaded_by,
                    downloaded_at
                FROM pulse.token_download
                WHERE file_identifier = ?
                ORDER BY downloaded_at ASC
                """;
        final List<Traceability> traceabilities = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileIdentifier.id());
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    traceabilities.add(
                            new Traceability(
                                    new Token(resultSet.getObject("token", UUID.class)),
                                    new FileIdentifier(resultSet.getString("file_identifier")),
                                    new DownloadedBy(resultSet.getString("downloaded_by")),
                                    new DownloadedAt(resultSet.getObject("downloaded_at", OffsetDateTime.class)
                                            .toZonedDateTime())
                            )
                    );
                }
            }
            return Collections.unmodifiableList(traceabilities);
        } catch (final SQLException exception) {
            throw new TokenRepositoryException(exception);
        }
    }
}

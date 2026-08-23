package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.Validate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class FileQuery {

    @Inject
    DataSource dataSource;

    public TokenDownload retrieveLastToken(final FileIdentifier fileIdentifier) {
        final AtomicReference<TokenDownload> token = new AtomicReference<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("SELECT token, file_identifier, downloaded_by, downloaded_at FROM pulse.token_download WHERE file_identifier = ?")) {
            statement.setString(1, fileIdentifier.id());
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    token.set(
                            new TokenDownload(resultSet.getString("token"),
                                    resultSet.getString("file_identifier"),
                                    resultSet.getString("downloaded_by"),
                                    resultSet.getString("downloaded_at")));
                }
            }
            Validate.validState(token.get() != null);
            return token.get();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

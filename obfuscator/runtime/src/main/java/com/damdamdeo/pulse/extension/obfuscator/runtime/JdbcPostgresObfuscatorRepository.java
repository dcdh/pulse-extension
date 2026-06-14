package com.damdamdeo.pulse.extension.obfuscator.runtime;

import com.damdamdeo.pulse.extension.core.obfuscator.UnableToDeObfuscateException;
import com.damdamdeo.pulse.extension.core.obfuscator.UnableToObfuscateException;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Unremovable
@DefaultBean
public class JdbcPostgresObfuscatorRepository implements ObfuscatorRepository {

    @Inject
    DataSource dataSource;

    private static final String INSERT_SQL = "INSERT INTO pulse.obfuscator (original, obfuscated) VALUES (?, ?)";

    private static final String SELECT_BY_ORIGINAL = "SELECT obfuscated FROM pulse.obfuscator WHERE original = ?";

    private static final String SELECT_BY_OBFUSCATED = "SELECT original FROM pulse.obfuscator WHERE obfuscated = ?";

    @Override
    public UUID store(final UUID obfuscated, final String value) throws UnableToObfuscateException {
        Objects.requireNonNull(obfuscated);
        Objects.requireNonNull(value);
        try (final Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ORIGINAL)) {
                ps.setString(1, value);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return UUID.fromString(rs.getString(1));
                    }
                }
            }
            try (final PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
                ps.setString(1, value);
                ps.setObject(2, obfuscated);
                ps.executeUpdate();
            }
            return obfuscated;
        } catch (final SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                return store(obfuscated, value);
            }
            throw new UnableToObfuscateException(e);
        }
    }

    @Override
    public Optional<String> retrieve(final UUID obfuscated) throws UnableToDeObfuscateException {
        Objects.requireNonNull(obfuscated);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(SELECT_BY_OBFUSCATED)) {
            ps.setObject(1, obfuscated);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString(1));
                }
            }
            return Optional.empty();
        } catch (final SQLException e) {
            throw new UnableToDeObfuscateException(e);
        }
    }
}

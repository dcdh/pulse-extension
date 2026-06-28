package com.damdamdeo.pulse.extension.writer.runtime;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@ApplicationScoped
@Transactional(TxType.MANDATORY)
@Unremovable
public class JdbcPostgresSequenceGenerator implements SequenceGenerator {

    public static final String HIMSELF = "himself";

    @Inject
    Provider<DataSource> dataSource;

    @Inject
    ApplicationNamingProvider applicationNamingProvider;

    @Override
    public <A extends Identifiable> SequenceNumber nextFor(final Class<A> identifiableClazz) throws SequenceGenerationException {
        Objects.requireNonNull(identifiableClazz);
        try (final Connection connection = dataSource.get().getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(
                    // language=sql
                    """
                            INSERT INTO pulse.sequences (
                                owned_by,
                                identifiable_clazz,
                                belongs_to,
                                next_value
                            )
                            VALUES (?, ?, ?, 1)
                            ON CONFLICT (identifiable_clazz, belongs_to)
                            DO UPDATE
                            SET owned_by = EXCLUDED.owned_by, next_value = pulse.sequences.next_value + 1
                            RETURNING next_value
                            """)) {
                preparedStatement.setString(1, applicationNamingProvider.provide().name());
                preparedStatement.setString(2, identifiableClazz.getSimpleName());
                preparedStatement.setString(3, HIMSELF);
                try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                    resultSet.next();
                    return SequenceNumber.fromNumber(resultSet.getLong("next_value"));
                }
            }
        } catch (final SQLException exception) {
            throw new SequenceGenerationException(exception);
        }
    }

    @Override
    public <A extends Identifiable> SequenceNumber nextFor(final For<A> identifiable) throws SequenceGenerationException {
        Objects.requireNonNull(identifiable);
        if (HIMSELF.equals(identifiable.belongsTo().id())) {
            throw new SequenceGenerationException(new IllegalArgumentException("Himself is not allowed as belongs_to in For"));
        }
        try (final Connection connection = dataSource.get().getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(
                    // language=sql
                    """
                            INSERT INTO pulse.sequences (
                                owned_by,
                                identifiable_clazz,
                                belongs_to,
                                next_value
                            )
                            VALUES (?, ?, ?, 1)
                            ON CONFLICT (identifiable_clazz, belongs_to)
                            DO UPDATE
                            SET owned_by = EXCLUDED.owned_by, next_value = pulse.sequences.next_value + 1
                            RETURNING next_value
                            """)) {
                preparedStatement.setString(1, applicationNamingProvider.provide().name());
                preparedStatement.setString(2, identifiable.identifiableClazz().getSimpleName());
                preparedStatement.setString(3, identifiable.belongsTo().id());
                try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                    resultSet.next();
                    return SequenceNumber.fromNumber(resultSet.getLong("next_value"));
                }
            }
        } catch (final SQLException exception) {
            throw new SequenceGenerationException(exception);
        }
    }
}

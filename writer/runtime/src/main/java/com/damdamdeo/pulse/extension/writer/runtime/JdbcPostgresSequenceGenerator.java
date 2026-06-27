package com.damdamdeo.pulse.extension.writer.runtime;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.apache.commons.text.CaseUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

    private static final String SEQUENCE_PREFIX = "seq_";

    @Inject
    Provider<DataSource> dataSource;

    @ConfigProperty(name = "quarkus.application.name")
    String quarkusApplicationName;

    public static String sequenceNameFor(final Class<? extends Identifiable> identifiableClazz) {
        return SEQUENCE_PREFIX + CaseUtils.toCamelCase(identifiableClazz.getSimpleName(), false, '_');
    }

    @Override
    public <A extends Identifiable> SequenceNumber nextFor(final Class<A> identifiableClazz) throws SequenceGenerationException {
        Objects.requireNonNull(identifiableClazz);
        final String sequenceName = sequenceNameFor(identifiableClazz);
        try (final Connection connection = dataSource.get().getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement("SELECT nextval(?)")) {
                preparedStatement.setString(1, sequenceName);
                try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SequenceGenerationException("Unable to retrieve next sequence value for '%s'".formatted(sequenceName),
                                sequenceName);
                    }
                    return SequenceNumber.fromNumber(resultSet.getLong(1));
                }
            }
        } catch (final SQLException exception) {
            throw new SequenceGenerationException("Unable to generate sequence number for '%s'".formatted(sequenceName),
                    exception, sequenceName);
        }
    }

    private static final String FOR_SEQUENCE_GENERATION_EXCEPTION_MESSAGE = "Unable to retrieve next sequence value for identifiable '%s' and and belongs to '%s'";
    private static final String FOR_SEQUENCE_NAME_EXCEPTION = "identifiable '%s' and belongs to '%s'";

    @Override
    public <A extends Identifiable> SequenceNumber nextFor(final For<A> identifiable) throws SequenceGenerationException {
        Objects.requireNonNull(identifiable);
        try (final Connection connection = dataSource.get().getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement("SELECT next_sequence_by_identifiable_clazz_and_belongs_to_value(?,?)")) {
                preparedStatement.setString(1, identifiable.identifiableClazz().getSimpleName());
                preparedStatement.setString(2, identifiable.belongsTo().id());
                try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SequenceGenerationException(FOR_SEQUENCE_GENERATION_EXCEPTION_MESSAGE.formatted(identifiable.identifiableClazz().getSimpleName(), identifiable.belongsTo().id()),
                                FOR_SEQUENCE_NAME_EXCEPTION.formatted(identifiable.identifiableClazz().getSimpleName(), identifiable.belongsTo().id()));
                    }
                    return SequenceNumber.fromNumber(resultSet.getLong(1));
                }
            }
        } catch (final SQLException exception) {
            throw new SequenceGenerationException(FOR_SEQUENCE_GENERATION_EXCEPTION_MESSAGE
                    .formatted(identifiable.identifiableClazz().getSimpleName(), identifiable.belongsTo().id()), exception,
                    FOR_SEQUENCE_NAME_EXCEPTION.formatted(identifiable.identifiableClazz().getSimpleName(), identifiable.belongsTo().id()));
        }
    }
}

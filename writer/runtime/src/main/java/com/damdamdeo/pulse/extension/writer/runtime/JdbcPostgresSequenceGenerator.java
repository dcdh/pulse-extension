package com.damdamdeo.pulse.extension.writer.runtime;

import com.damdamdeo.pulse.extension.core.SequenceGenerationException;
import com.damdamdeo.pulse.extension.core.SequenceGenerator;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.apache.commons.text.CaseUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresSequenceGenerator implements SequenceGenerator {

    private static final String SEQUENCE_PREFIX = "seq_";

    @Inject
    Provider<DataSource> dataSource;

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
}

package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.ApplicationNamingProvider;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import com.damdamdeo.pulse.extension.core.traceability.CorrelationId;
import com.damdamdeo.pulse.extension.core.traceability.CorrelationIdGenerator;
import com.damdamdeo.pulse.extension.core.traceability.CorrelationIdGeneratorException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.Validate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresCorrelationIdGenerator implements CorrelationIdGenerator {

    private final DataSource dataSource;
    private final SchemaName schemaName;

    public JdbcPostgresCorrelationIdGenerator(final DataSource dataSource,
                                              final ApplicationNamingProvider applicationNamingProvider) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.schemaName = SchemaName.from(applicationNamingProvider.provide());
    }

    @Override
    public CorrelationId generate() throws CorrelationIdGeneratorException {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(
                     "SELECT nextval('%s.correlation_id_seq')".formatted(schemaName.name()))) {
            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new CorrelationIdGeneratorException(new IllegalStateException("Unable to generate correlation ID"));
                }
                final CorrelationId correlationId = new CorrelationId(resultSet.getLong(1));
                Validate.validState(!CorrelationId.NOT_AVAILABLE.equals(correlationId));
                return correlationId;
            }
        } catch (final SQLException exception) {
            throw new CorrelationIdGeneratorException(exception);
        }
    }
}

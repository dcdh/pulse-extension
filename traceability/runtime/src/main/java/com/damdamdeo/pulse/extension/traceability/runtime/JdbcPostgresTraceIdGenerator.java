package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.ApplicationNamingProvider;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import com.damdamdeo.pulse.extension.core.traceability.TraceId;
import com.damdamdeo.pulse.extension.core.traceability.TraceIdGenerator;
import com.damdamdeo.pulse.extension.core.traceability.TraceIdGeneratorException;
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
public class JdbcPostgresTraceIdGenerator implements TraceIdGenerator {

    private final DataSource dataSource;
    private final SchemaName schemaName;

    public JdbcPostgresTraceIdGenerator(final DataSource dataSource,
                                        final ApplicationNamingProvider applicationNamingProvider) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.schemaName = SchemaName.from(applicationNamingProvider.provide());
    }

    @Override
    public TraceId generate() throws TraceIdGeneratorException {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(
                     "SELECT nextval('%s.trace_id_seq')".formatted(schemaName.name()))) {
            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new TraceIdGeneratorException(new IllegalStateException("Unable to generate trace ID"));
                }
                final TraceId traceId = new TraceId(resultSet.getLong(1));
                Validate.validState(!TraceId.NOT_AVAILABLE.equals(traceId));
                return traceId;
            }
        } catch (final SQLException exception) {
            throw new TraceIdGeneratorException(exception);
        }
    }
}

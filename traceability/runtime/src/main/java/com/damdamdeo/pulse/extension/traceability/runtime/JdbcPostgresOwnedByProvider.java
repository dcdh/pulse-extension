package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.traceability.OwnedByProvider;
import com.damdamdeo.pulse.extension.core.traceability.OwnedByProviderException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresOwnedByProvider implements OwnedByProvider {

    // language=sql
    public static final String PROVIDE_SQL = """
            SELECT e.owned_by AS owned_by FROM event e WHERE e.aggregate_root_id = ? AND e.aggregate_root_type = ? LIMIT 1
            """;

    private final DataSource dataSource;

    public JdbcPostgresOwnedByProvider(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public OwnedBy provide(final AnyAggregateId aggregateId) throws OwnedByProviderException {
        Objects.requireNonNull(aggregateId);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(PROVIDE_SQL)) {
            preparedStatement.setString(1, aggregateId.id());
            preparedStatement.setString(2, aggregateId.simpleName());
            final var resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new OwnedBy(resultSet.getString("owned_by"));
            } else {
                throw new OwnedByProviderException(new IllegalStateException("Should not happen, the event should have been persisted before."));
            }
        } catch (final SQLException exception) {
            throw new OwnedByProviderException(exception);
        }
    }
}

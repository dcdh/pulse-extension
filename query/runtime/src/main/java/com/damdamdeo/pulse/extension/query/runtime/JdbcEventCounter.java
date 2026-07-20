package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@ApplicationScoped
public class JdbcEventCounter implements EventCounter {

    public static final String OWNED_BY = "OWNED_BY";
    public static final String AGGREGATE_ID = "AGGREGATE_ID";

    @Inject
    DataSource dataSource;

    @Override
    public Integer byOwnedBy(final OwnedBy ownedBy) throws EventCounterException {
        Objects.requireNonNull(ownedBy);
        try {
            return count(OWNED_BY, ownedBy.id());
        } catch (final SQLException e) {
            throw new EventCounterException(e);
        }
    }

    @Override
    public Integer byAggregateId(final AggregateId aggregateId) throws EventCounterException {
        Objects.requireNonNull(aggregateId);
        try {
            return count(AGGREGATE_ID, aggregateId.id());
        } catch (final SQLException e) {
            throw new EventCounterException(e);
        }
    }

    private Integer count(final String counterType, final String counterId) throws SQLException {
        Objects.requireNonNull(counterType);
        Objects.requireNonNull(counterId);
        // language=sql
        final String sql = """
                SELECT event_count FROM event_counter WHERE counter_type = ? AND counter_id = ?
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, counterType);
            statement.setString(2, counterId);
            try (final ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}

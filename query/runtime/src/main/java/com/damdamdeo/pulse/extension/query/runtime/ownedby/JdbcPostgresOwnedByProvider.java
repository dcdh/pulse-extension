package com.damdamdeo.pulse.extension.query.runtime.ownedby;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ApplicationScoped
@Transactional
public class JdbcPostgresOwnedByProvider implements OwnedByProvider {

    @Inject
    DataSource dataSource;

    @Override
    public OwnedBy getByAggregateId(final AggregateId aggregateId) throws UnableToProvideOwnedByException {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT owned_by FROM aggregate_root WHERE aggregate_root_id = ?
                             """)) {
            ps.setString(1, aggregateId.id());
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return OwnedBy.from(new AnyAggregateId(rs.getString("owned_by")));
                }
            }
            throw new UnableToProvideOwnedByException(new UnknownOwnedBy());
        } catch (final SQLException e) {
            throw new UnableToProvideOwnedByException(e);
        }
    }
}

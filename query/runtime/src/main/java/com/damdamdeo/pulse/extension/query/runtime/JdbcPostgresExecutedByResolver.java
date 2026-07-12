package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByFactory;
import com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException;
import com.damdamdeo.pulse.extension.core.query.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
@DefaultBean
public final class JdbcPostgresExecutedByResolver implements ExecutedByResolver {

    @Inject
    DataSource dataSource;

    @Inject
    ExecutedByFactory executedByFactory;

    @Override
    public Set<ExecutedBy> resolve(final Set<AggregateId> aggregatesId) throws UnableToResolveException {
        Objects.requireNonNull(aggregatesId);
        if (aggregatesId.isEmpty()) {
            return Set.of();
        }
        final Set<ExecutedBy> setOfExecutedBy = new HashSet<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement("SELECT executed_by, owned_by FROM aggregate_executed_by WHERE aggregate_root_id = ANY(?::varchar[])")) {
            final Array listOfAggregateIdArray = connection.createArrayOf("varchar", aggregatesId.stream()
                    .map(Identifiable::id)
                    .toArray(String[]::new));
            ps.setArray(1, listOfAggregateIdArray);
            try (final ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    final String executedBy = resultSet.getString("executed_by");
                    final String ownedBy = resultSet.getString("owned_by");
                    setOfExecutedBy.add(executedByFactory.from(executedBy, new OwnedBy(ownedBy)));
                }
            }
            return setOfExecutedBy;
        } catch (final UnableToDecodeException | SQLException e) {
            throw new UnableToResolveException(e);
        }
    }
}

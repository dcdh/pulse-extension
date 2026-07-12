package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.ExecutedByResolver;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
@DefaultBean
public final class JdbcPostgresExecutedByResolver implements ExecutedByResolver {

    @Inject
    DataSource dataSource;

    @Override
    public Set<ExecutedBy> resolve(final Set<AggregateId> aggregatesId) {
        Objects.requireNonNull(aggregatesId);
        if (aggregatesId.isEmpty()) {
            return Set.of();
        }
        throw new RuntimeException("not implemented");
    }
}

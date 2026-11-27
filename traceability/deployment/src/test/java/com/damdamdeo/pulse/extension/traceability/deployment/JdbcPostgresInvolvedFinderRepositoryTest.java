package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresInvolvedFinderRepository;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcPostgresInvolvedFinderRepositoryTest extends AbstractTraceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresInvolvedFinderRepository jdbcPostgresInvolvedFinderRepository;

    @Test
    void shouldFindByIdentifiable() throws TraceRepositoryException {
        // Given
        final List<EncodedAggregateIdInvolved> executions = new ArrayList<>(2);

        // When
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(TodoId.USER_1_TODO_1, new Pagination(0, 1)));
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(TodoId.USER_1_TODO_1, new Pagination(1, 1)));
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(TodoId.USER_1_TODO_1, new Pagination(0, -1)));

        assertThat(executions).containsExactly(
                new EncodedAggregateIdInvolved(TodoId.USER_1_TODO_1, new Page<>(List.of(), new Pagination(0, 1), 0), Set.of()),
                new EncodedAggregateIdInvolved(TodoId.USER_1_TODO_1, new Page<>(List.of(), new Pagination(1, 1), 0), Set.of()),
                new EncodedAggregateIdInvolved(TodoId.USER_1_TODO_1, new Page<>(List.of(), new Pagination(0, -1), 0), Set.of())
        );
    }

    @Test
    void shouldFindByExecutedByHashed() throws TraceRepositoryException {
        // Given
        final List<EncodedExecutedByInvolved> executions = new ArrayList<>(2);

        // When
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(new ExecutedByHashed("EU:bob-hashed"), new Pagination(0, 1)));
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(new ExecutedByHashed("EU:bob-hashed"), new Pagination(1, 1)));
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(new ExecutedByHashed("EU:bob-hashed"), new Pagination(0, -1)));
        executions.add(jdbcPostgresInvolvedFinderRepository.findBy(new ExecutedByHashed("EU:alice-hashed"), new Pagination(0, -1)));

        // Then
        assertThat(executions).extracting(EncodedExecutedByInvolved::page).containsExactly(
                new Page<>(List.of(), new Pagination(0, 1), 0),
                new Page<>(List.of(), new Pagination(1, 1), 0),
                new Page<>(List.of(), new Pagination(0, -1), 0),
                new Page<>(List.of(), new Pagination(0, -1), 0)
        );
    }
}

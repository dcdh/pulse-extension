package com.damdamdeo.pulse.extension.traceability.deployment.finder;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.traceability.AggregateIdInvolved;
import com.damdamdeo.pulse.extension.core.traceability.DefaultInvolvedFinder;
import com.damdamdeo.pulse.extension.core.traceability.ExecutedByInvolved;
import com.damdamdeo.pulse.extension.core.traceability.FinderException;
import com.damdamdeo.pulse.extension.traceability.deployment.AbstractTraceTest;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultInvolvedFinderTest extends AbstractTraceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubUsernameEncoder.class,
                    StubOwnedByProvider.class, StubExecutionContextProvider.class, StubUsernameDecoder.class))
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED")
            .withConfigurationResource("application.properties");

    @Inject
    DefaultInvolvedFinder defaultInvolvedFinder;

    @Test
    @RunOnVertxContext
    void shouldFindByIdenifiable() throws FinderException {
        // Given

        // When
        final AggregateIdInvolved by = defaultInvolvedFinder.findBy(TodoId.USER_1_TODO_1, new Pagination(0, 10));

        // Then
        assertThat(by).isEqualTo(new AggregateIdInvolved(TodoId.USER_1_TODO_1,
                AnyAggregateId.from(TodoId.USER_1_TODO_1), new Page<>(List.of(), new Pagination(0, 10), 0),
                List.of()));
    }

    @Test
    @RunOnVertxContext
    void shouldFindByExecutedByHashed() throws FinderException {
        // Given

        // When
        final ExecutedByInvolved by = defaultInvolvedFinder.findBy(new ExecutedByHashed("EU:alice-hashed"), new Pagination(0, 10));

        // Then
        assertThat(by).isInstanceOfSatisfying(ExecutedByInvolved.class, involved ->
                assertThat(involved.page()).isEqualTo(new Page<>(List.of(), new Pagination(0, 10), 0)));
    }
}

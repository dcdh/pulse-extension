package com.damdamdeo.pulse.extension.traceability.deployment.finder;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultDetailedInvolvedFinderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubUsernameEncoder.class,
                    StubOwnedByProvider.class, StubExecutionContextProvider.class, StubUsernameDecoder.class))
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Inject
    DefaultDetailedInvolvedFinder defaultDetailedInvolvedFinder;

    @Inject
    JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository jdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository;

    @BeforeAll
    void prepare() throws TraceRepositoryException {
        for (final TraceRecorder traceRecorder : List.of(
                new TraceRecorder(
                        new TraceId(1L),
                        new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")),
                        new From("from"),
                        List.of(
                                new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded"))))
        )) {
            jdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository.store(traceRecorder);
        }
    }

    @Test
    void shouldFindByAggregateId() throws FinderException {
        // Given

        // When
        final Page<DetailedInvolved> by = defaultDetailedInvolvedFinder.findBy(TodoId.USER_1_TODO_1, new Pagination(0, 10));

        // Then
        assertThat(by).isEqualTo(new Page<>(
                List.of(
                        new DetailedInvolved(new TraceId(1L), new Involved(TodoId.USER_1_TODO_1, new ExecutedByHashed("EU:alice-hashed"), new ExecutedBy.EndUser(new Username("alice@mail.com"))),
                                new From("from"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"))),
                        new DetailedInvolved(new TraceId(1L), new Involved(TodoId.USER_1_TODO_1, new ExecutedByHashed("EU:bob-hashed"), new ExecutedBy.EndUser(new Username("bob@mail.com"))),
                                new From("from"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")))),
                new Pagination(0, 10), 2L));
    }

    @Test
    void shouldFindByExecutedByHashed() throws FinderException {
        // Given

        // When
        final Page<DetailedInvolved> by = defaultDetailedInvolvedFinder.findBy(new ExecutedByHashed("EU:alice-hashed"), new Pagination(0, 10));

        // Then
        assertThat(by).isEqualTo(new Page<>(
                List.of(
                        new DetailedInvolved(new TraceId(1L), new Involved(new AnyAggregateId("U000001-T000001"), new ExecutedByHashed("EU:alice-hashed"), new ExecutedBy.EndUser(new Username("alice@mail.com"))),
                                new From("from"), new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")))),
                new Pagination(0, 10), 1L));
    }
}

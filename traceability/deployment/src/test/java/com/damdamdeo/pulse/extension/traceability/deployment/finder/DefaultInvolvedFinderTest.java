package com.damdamdeo.pulse.extension.traceability.deployment.finder;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresInvolvedTraceRecorderRepository;
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
class DefaultInvolvedFinderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubUsernameEncoder.class,
                    StubOwnedByProvider.class, StubExecutionContextProvider.class, StubUsernameDecoder.class))
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED")
            .withConfigurationResource("application.properties");

    @Inject
    DefaultInvolvedFinder defaultInvolvedFinder;

    @Inject
    JdbcPostgresInvolvedTraceRecorderRepository jdbcPostgresInvolvedTraceRecorderRepository;

    @BeforeAll
    void prepare() throws TraceRepositoryException {
        for (final TraceRecorder traceRecorder : List.of(
                new TraceRecorder(
                        new TraceId(0L),
                        new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")),
                        Source.COMMAND,
                        new From("from"),
                        List.of(
                                new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                                new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded"))))
        )) {
            jdbcPostgresInvolvedTraceRecorderRepository.store(traceRecorder);
        }
    }

    @Test
    void shouldFindByAggregateId() throws FinderException {
        // Given

        // When
        final Page<Involved> by = defaultInvolvedFinder.findBy(TodoId.USER_1_TODO_1, new IncludeUncompounded(false), new Pagination(0, 10));

        // Then
        assertThat(by).isEqualTo(new Page<>(
                List.of(
                        new Involved(AnyAggregateId.from(TodoId.USER_1_TODO_1), new Actor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedBy.EndUser(new Username("alice@mail.com"))),
                                new CommandNbOfTimes(1), new QueryNbOfTimes(0)),
                        new Involved(AnyAggregateId.from(TodoId.USER_1_TODO_1), new Actor(new ExecutedByHashed("EU:bob-hashed"), new ExecutedBy.EndUser(new Username("bob@mail.com"))),
                                new CommandNbOfTimes(1), new QueryNbOfTimes(0))),
                new Pagination(0, 10), 2L));
    }

    @Test
    void shouldFindByExecutedByHashed() throws FinderException {
        // Given

        // When
        final Page<Involved> by = defaultInvolvedFinder.findBy(new ExecutedByHashed("EU:alice-hashed"), new Pagination(0, 10));

        // Then
        assertThat(by).isEqualTo(new Page<>(
                List.of(
                        new Involved(AnyAggregateId.from("U000001-T000001"), new Actor(new ExecutedByHashed("EU:alice-hashed"), new ExecutedBy.EndUser(new Username("alice@mail.com"))),
                                new CommandNbOfTimes(1), new QueryNbOfTimes(0))),
                new Pagination(0, 10), 1L));
    }
}

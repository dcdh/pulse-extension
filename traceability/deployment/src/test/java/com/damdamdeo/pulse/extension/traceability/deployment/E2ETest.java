package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.TestUsernameEncoder;
import com.damdamdeo.pulse.extension.core.executedby.UnableToEncodeException;
import com.damdamdeo.pulse.extension.core.query.SampleInput;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.deployment.finder.StubOwnedByProvider;
import com.damdamdeo.pulse.extension.traceability.deployment.finder.StubUsernameDecoder;
import com.damdamdeo.pulse.extension.traceability.deployment.finder.StubUsernameEncoder;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.damdamdeo.pulse.extension.core.traceability.Finder.ROLE_TRACEABILITY_READ;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class E2ETest {

    private static ExecutedBy BOB = new ExecutedBy.EndUser(new Username("bob@mail.com"));

    private static ExecutedBy ALICE = new ExecutedBy.EndUser(new Username("alice@mail.com"));

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubUsernameEncoder.class,
                    StubOwnedByProvider.class,
                    StubUsernameDecoder.class))
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @ApplicationScoped
    @Priority(1)
    @Alternative
    static class StubExecutedAtProvider implements ExecutedAtProvider {

        @Override
        public ExecutedAt now() {
            return new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z"));
        }
    }

    @ApplicationScoped
    @Priority(1)
    @Alternative
    static class StubExecutionContextProvider implements ExecutionContextProvider {

        private ExecutedBy.EndUser endUser;

        @Override
        public ExecutionContext provide() {
            if (endUser == null) {
                throw new IllegalStateException("endUser not set");
            }
            return new ExecutionContext(endUser, Set.of(ROLE_TRACEABILITY_READ));
        }

        public void set(final ExecutedBy.EndUser endUser) {
            this.endUser = Objects.requireNonNull(endUser);
        }
    }


    @Inject
    DataSource dataSource;

    @Inject
    TraceAppender traceAppender;

    @Inject
    StubExecutionContextProvider stubExecutionContextProvider;

    @BeforeAll
    void prepareDatasource() {
        // language=sql
        final String table = """
                CREATE TABLE IF NOT EXISTS todo_taking.event (
                  aggregate_root_type character varying(255) not null,
                  aggregate_root_id character varying(255) not null,
                  version bigint not null,
                  stored_at timestamptz not null,
                  event_type character varying(255) not null,
                  event_payload bytea not null CHECK (octet_length(event_payload) <= 1000 * 1024),
                  owned_by character varying(255) not null,
                  belongs_to character varying(255) not null,
                  executed_by character varying(255) not null,
                  CONSTRAINT event_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type, version),
                  CONSTRAINT event_unique UNIQUE (aggregate_root_id, aggregate_root_type, version),
                  CONSTRAINT executed_by_format_chk CHECK (executed_by = 'A' OR executed_by LIKE 'EU:%' OR executed_by LIKE 'SA:%' OR executed_by = 'NA')
                );
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(table)) {
            preparedStatement.executeUpdate();
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    record SimpleCommand(TodoId id) implements Command<TodoId> {

        SimpleCommand {
            Objects.requireNonNull(id);
        }
    }

    @Test
    @RunOnVertxContext
    void shouldStoreAndRetrieveTrace() throws TraceAppenderException, SQLException {
        // Given
        insertEvent(TodoId.USER_1_TODO_1.id(), "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                Todo.OWNED_BY_USER_1, ALICE);
        insertEvent(TodoId.USER_1_TODO_1.id(), "Todo", 1,
                Instant.parse("2025-10-13T19:00:00Z"), "TodoDescriptionUpdated", "\\x",
                Todo.OWNED_BY_USER_1, ALICE);
        insertEvent(TodoId.USER_1_TODO_1.id(), "Todo", 2,
                Instant.parse("2025-10-13T21:00:00Z"), "MarkTodoAsDone", "\\x",
                Todo.OWNED_BY_USER_1, BOB);
        insertEvent(TodoId.USER_1_TODO_2.id(), "Todo", 0,
                Instant.parse("2025-10-13T22:00:00Z"), "NewTodoCreated", "\\x",
                Todo.OWNED_BY_USER_1, ALICE);

        // When
        // Alice
        stubExecutionContextProvider.set(new ExecutedBy.EndUser(new Username("alice@mail.com")));
        traceAppender.append(new SimpleCommand(TodoId.USER_1_TODO_1), List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ALICE)),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new ExecutedByEvent<>(new TodoDescriptionUpdated("lorem ipsum"), ALICE))
        ), ExecutionStatus.SUCCESS);
        // Bob
        stubExecutionContextProvider.set(new ExecutedBy.EndUser(new Username("bob@mail.com")));
        traceAppender.append(new SimpleCommand(TodoId.USER_1_TODO_1), List.of(
                new VersionizedEvent<>(new AggregateVersion(2),
                        new ExecutedByEvent<>(new TodoMarkedAsDone(), BOB))
        ), ExecutionStatus.SUCCESS);
        traceAppender.append(new SampleInput(), Set.of(TodoId.USER_1_TODO_1), ExecutionStatus.FAILED_UNAUTHORIZED);
        // Alice
        stubExecutionContextProvider.set(new ExecutedBy.EndUser(new Username("alice@mail.com")));
        traceAppender.append(new SimpleCommand(TodoId.USER_1_TODO_2), List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ALICE))
        ), ExecutionStatus.SUCCESS);

        // Then
        given()
                .pathParam("aggregateId", "U000001-T000001")
                .queryParam("includeUncompounded", "true")
                .queryParam("page[index]", "0")
                .queryParam("page[size]", "10")
                .when()
                .get("/traceability/finder/involved/byAggregateId/{aggregateId}")
                .then()
                .log().all()
                .statusCode(200)
                .body("listOfInvolved.size()", equalTo(2))
                .body("listOfInvolved[0].aggregateId", equalTo("U000001-T000001"))
                .body("listOfInvolved[0].executedByHashed", equalTo("EU:4714636ab5e7b6ec200c9a0ec8a1b08f61df989c47f22f9e9322adf63922d9e4"))
                .body("listOfInvolved[0].executedBy", equalTo("EU:alice@mail.com"))
                .body("listOfInvolved[0].commandNbOfTimes", equalTo(2))
                .body("listOfInvolved[0].commandUnauthorizedNbOfTimes", equalTo(0))
                .body("listOfInvolved[0].commandBusinessFailedNbOfTimes", equalTo(0))
                .body("listOfInvolved[0].queryNbOfTimes", equalTo(0))
                .body("listOfInvolved[0].queryUnauthorizedNbOfTimes", equalTo(0))
                .body("listOfInvolved[1].aggregateId", equalTo("U000001-T000001"))
                .body("listOfInvolved[1].executedByHashed", equalTo("EU:d05761c6486e77a8efdb4c5149f84ef0b20abd2454f66a91d7cbd52d71201976"))
                .body("listOfInvolved[1].executedBy", equalTo("EU:bob@mail.com"))
                .body("listOfInvolved[1].commandNbOfTimes", equalTo(1))
                .body("listOfInvolved[1].commandUnauthorizedNbOfTimes", equalTo(0))
                .body("listOfInvolved[1].commandBusinessFailedNbOfTimes", equalTo(0))
                .body("listOfInvolved[1].queryNbOfTimes", equalTo(0))
                .body("listOfInvolved[1].queryUnauthorizedNbOfTimes", equalTo(1))
                .body("totalPages", equalTo(1))
                .body("hasNext", equalTo(false))
                .body("hasPrevious", equalTo(false));
        given()
                .pathParam("aggregateId", "U000001-T000001")
                .queryParam("includeUncompounded", "true")
                .queryParam("page[index]", "0")
                .queryParam("page[size]", "10")
                .when()
                .get("/traceability/finder/detailed/byAggregateId/{aggregateId}")
                .then()
                .log().all()
                .statusCode(200)
                .body("listOfInvolved.size()", equalTo(4))

                .body("listOfInvolved[0].traceId", equalTo(1))
                .body("listOfInvolved[0].correlationId", equalTo(1))
                .body("listOfInvolved[0].aggregateId", equalTo("U000001-T000001"))
                .body("listOfInvolved[0].executedByHashed", equalTo("EU:4714636ab5e7b6ec200c9a0ec8a1b08f61df989c47f22f9e9322adf63922d9e4"))
                .body("listOfInvolved[0].executedBy", equalTo("EU:alice@mail.com"))
                .body("listOfInvolved[0].eventType", equalTo("NewTodoCreated"))
                .body("listOfInvolved[0].aggregateVersion", equalTo(0))
                .body("listOfInvolved[0].source", equalTo("COMMAND"))
                .body("listOfInvolved[0].executionStatus", equalTo("SUCCESS"))
                .body("listOfInvolved[0].from", equalTo("SimpleCommand"))
                .body("listOfInvolved[0].executedAt", equalTo("2026-09-06T12:00:00Z"))

                .body("listOfInvolved[1].traceId", equalTo(1))
                .body("listOfInvolved[1].correlationId", equalTo(1))
                .body("listOfInvolved[1].aggregateId", equalTo("U000001-T000001"))
                .body("listOfInvolved[1].executedByHashed", equalTo("EU:4714636ab5e7b6ec200c9a0ec8a1b08f61df989c47f22f9e9322adf63922d9e4"))
                .body("listOfInvolved[1].executedBy", equalTo("EU:alice@mail.com"))
                .body("listOfInvolved[1].eventType", equalTo("TodoDescriptionUpdated"))
                .body("listOfInvolved[1].aggregateVersion", equalTo(1))
                .body("listOfInvolved[1].source", equalTo("COMMAND"))
                .body("listOfInvolved[1].executionStatus", equalTo("SUCCESS"))
                .body("listOfInvolved[1].from", equalTo("SimpleCommand"))
                .body("listOfInvolved[1].executedAt", equalTo("2026-09-06T12:00:00Z"))

                .body("listOfInvolved[2].traceId", equalTo(2))
                .body("listOfInvolved[2].correlationId", equalTo(1))
                .body("listOfInvolved[2].aggregateId", equalTo("U000001-T000001"))
                .body("listOfInvolved[2].executedByHashed", equalTo("EU:d05761c6486e77a8efdb4c5149f84ef0b20abd2454f66a91d7cbd52d71201976"))
                .body("listOfInvolved[2].executedBy", equalTo("EU:bob@mail.com"))
                .body("listOfInvolved[2].eventType", equalTo("TodoMarkedAsDone"))
                .body("listOfInvolved[2].aggregateVersion", equalTo(2))
                .body("listOfInvolved[2].source", equalTo("COMMAND"))
                .body("listOfInvolved[2].executionStatus", equalTo("SUCCESS"))
                .body("listOfInvolved[2].from", equalTo("SimpleCommand"))
                .body("listOfInvolved[2].executedAt", equalTo("2026-09-06T12:00:00Z"))

                .body("listOfInvolved[3].traceId", equalTo(3))
                .body("listOfInvolved[3].correlationId", equalTo(1))
                .body("listOfInvolved[3].aggregateId", equalTo("U000001-T000001"))
                .body("listOfInvolved[3].executedByHashed", equalTo("EU:d05761c6486e77a8efdb4c5149f84ef0b20abd2454f66a91d7cbd52d71201976"))
                .body("listOfInvolved[3].executedBy", equalTo("EU:bob@mail.com"))
                .body("listOfInvolved[3].eventType", nullValue())
                .body("listOfInvolved[3].aggregateVersion", nullValue())
                .body("listOfInvolved[3].source", equalTo("QUERY"))
                .body("listOfInvolved[3].executionStatus", equalTo("FAILED_UNAUTHORIZED"))
                .body("listOfInvolved[3].from", equalTo("SampleInput"))
                .body("listOfInvolved[3].executedAt", equalTo("2026-09-06T12:00:00Z"))

                .body("totalPages", equalTo(1))
                .body("hasNext", equalTo(false))
                .body("hasPrevious", equalTo(false))
        ;

        final List<String> data = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement selectTraceabilityDetailsPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT trace_id, correlation_id, executed_at, source_value, execution_status, from_value FROM todo_taking.traceability_details
                             """
             );
             final PreparedStatement selectExecutedByEncodedPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT id, executed_by_hashed, executed_by_encoded FROM todo_taking.executed_by_encoded
                             """
             );
             final PreparedStatement selectTraceabilityAggregatePreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT id, aggregate_root_id, executed_by_encoded_id, command_nb_of_times, command_unauthorized_nb_of_times, command_business_failed_nb_of_times, query_nb_of_times, query_unauthorized_nb_of_times FROM todo_taking.traceability_aggregate
                             """);
             final PreparedStatement selectTraceabilityDetailsTraceabilityAggregatePreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT traceability_details_id, traceability_aggregate_id FROM todo_taking.traceability_details_traceability_aggregate
                             """
             );
             final PreparedStatement selectTraceabilityAggregateEventsPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT id, traceability_trace_id, event_type, aggregate_version FROM todo_taking.traceability_aggregate_events
                             """
             )) {
            ResultSet resultSet = selectTraceabilityDetailsPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|",
                        "traceability_details",
                        resultSet.getString("trace_id"),
                        resultSet.getString("correlation_id"),
                        resultSet.getString("executed_at"),
                        String.valueOf(resultSet.getInt("source_value")),
                        String.valueOf(resultSet.getInt("execution_status")),
                        resultSet.getString("from_value")));
            }
            resultSet = selectExecutedByEncodedPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|",
                        "executed_by_encoded",
                        resultSet.getString("id"), resultSet.getString("executed_by_hashed"),
                        resultSet.getString("executed_by_encoded")));
            }
            resultSet = selectTraceabilityAggregatePreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|",
                        "traceability_aggregate",
                        resultSet.getString("id"), resultSet.getString("aggregate_root_id"),
                        resultSet.getString("executed_by_encoded_id"),
                        String.valueOf(resultSet.getLong("command_nb_of_times")),
                        String.valueOf(resultSet.getLong("command_unauthorized_nb_of_times")),
                        String.valueOf(resultSet.getLong("command_business_failed_nb_of_times")),
                        String.valueOf(resultSet.getLong("query_nb_of_times")),
                        String.valueOf(resultSet.getLong("query_unauthorized_nb_of_times"))));
            }
            resultSet = selectTraceabilityDetailsTraceabilityAggregatePreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|",
                        "traceability_details_traceability_aggregate",
                        resultSet.getString("traceability_details_id"),
                        resultSet.getString("traceability_aggregate_id")));
            }
            resultSet = selectTraceabilityAggregateEventsPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|",
                        "traceability_aggregate_events",
                        resultSet.getString("id"),
                        resultSet.getString("traceability_trace_id"),
                        resultSet.getString("event_type"),
                        String.valueOf(resultSet.getLong("aggregate_version"))));
            }
        }
        assertThat(data).containsExactly(
                "traceability_details|1|1|2026-09-06 14:00:00+02|0|0|SimpleCommand",
                "traceability_details|2|1|2026-09-06 14:00:00+02|0|0|SimpleCommand",
                "traceability_details|3|1|2026-09-06 14:00:00+02|1|1|SampleInput",
                "traceability_details|4|1|2026-09-06 14:00:00+02|0|0|SimpleCommand",
                "executed_by_encoded|1|EU:4714636ab5e7b6ec200c9a0ec8a1b08f61df989c47f22f9e9322adf63922d9e4|EU:aliceEncoded",
                "executed_by_encoded|4|EU:d05761c6486e77a8efdb4c5149f84ef0b20abd2454f66a91d7cbd52d71201976|EU:bobEncoded",
                "traceability_aggregate|1|U000001-T000001|1|2|0|0|0|0",
                "traceability_aggregate|3|U000001-T000001|4|1|0|0|0|1",
                "traceability_aggregate|5|U000001-T000002|1|1|0|0|0|0",
                "traceability_details_traceability_aggregate|1|1",
                "traceability_details_traceability_aggregate|2|3",
                "traceability_details_traceability_aggregate|3|3",
                "traceability_details_traceability_aggregate|4|5",
                "traceability_aggregate_events|1|1|NewTodoCreated|0",
                "traceability_aggregate_events|2|1|TodoDescriptionUpdated|1",
                "traceability_aggregate_events|3|2|TodoMarkedAsDone|2",
                "traceability_aggregate_events|4|4|NewTodoCreated|0");
    }

    private void insertEvent(final String aggregateRootId, final String aggregateRootType, final Integer version,
                             final Instant storedAt, final String eventType, final String encryptedEventPayload,
                             final OwnedBy ownedBy, final ExecutedBy executedBy) throws SQLException {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             INSERT INTO event (aggregate_root_id, aggregate_root_type, version, stored_at, event_type, event_payload, owned_by, belongs_to, executed_by) 
                             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                             """)) {
            preparedStatement.setString(1, aggregateRootId);
            preparedStatement.setString(2, aggregateRootType);
            preparedStatement.setLong(3, version);
            preparedStatement.setTimestamp(4, Timestamp.from(storedAt));
            preparedStatement.setString(5, eventType);
            preparedStatement.setBytes(6, encryptedEventPayload.getBytes(StandardCharsets.UTF_8));
            preparedStatement.setString(7, ownedBy.id());
            preparedStatement.setString(8, aggregateRootId);
            preparedStatement.setString(9, executedBy.encode(TestUsernameEncoder.INSTANCE, ownedBy).encoded());
            preparedStatement.executeUpdate();
        } catch (final UnableToEncodeException e) {
            throw new RuntimeException(e);
        }
    }
}

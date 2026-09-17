package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.TestUsernameEncoder;
import com.damdamdeo.pulse.extension.core.executedby.UnableToEncodeException;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.deployment.finder.StubExecutionContextProvider;
import com.damdamdeo.pulse.extension.traceability.deployment.finder.StubOwnedByProvider;
import com.damdamdeo.pulse.extension.traceability.deployment.finder.StubUsernameDecoder;
import com.damdamdeo.pulse.extension.traceability.deployment.finder.StubUsernameEncoder;
import io.quarkus.test.QuarkusUnitTest;
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
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class E2ETest {

    private static ExecutedBy BOB = new ExecutedBy.EndUser(new Username("bob@mail.com"));

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubUsernameEncoder.class,
                    StubOwnedByProvider.class, StubExecutionContextProvider.class, StubUsernameDecoder.class))
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

    @Inject
    DataSource dataSource;

    @Inject
    TraceAppender traceAppender;

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

    @Test
    void shouldStoreAndRetrieveTrace() throws TraceAppenderException, SQLException {
        // Given
        insertEvent(TodoId.USER_1_TODO_1.id(), "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                Todo.OWNED_BY_USER_1, BOB);

        // When
        traceAppender.append(new Traceable() {
            @Override
            public Set<AggregateId> aggregateIds() {
                return Set.of(TodoId.USER_1_TODO_1);
            }
        }, new From("shouldStoreAndRetrieveTrace"));

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
                .body("listOfInvolved.size()", equalTo(1));
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
                .body("listOfInvolved.size()", equalTo(1));
        final List<String> data = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement selectTraceabilityDetailsPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT trace_id, executed_at, from_value FROM todo_taking.traceability_details
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
                             SELECT id, aggregate_root_id, executed_by_encoded_id, nb_of_times FROM todo_taking.traceability_aggregate
                             """);
             final PreparedStatement selectTraceabilityDetailsTraceabilityAggregatePreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT traceability_details_id, traceability_aggregate_id FROM todo_taking.traceability_details_traceability_aggregate
                             """
             )) {
            ResultSet resultSet = selectTraceabilityDetailsPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("trace_id"), resultSet.getString("executed_at"),
                        resultSet.getString("from_value")));
            }
            resultSet = selectExecutedByEncodedPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("id"), resultSet.getString("executed_by_hashed"),
                        resultSet.getString("executed_by_encoded")));
            }
            resultSet = selectTraceabilityAggregatePreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("id"), resultSet.getString("aggregate_root_id"),
                        resultSet.getString("executed_by_encoded_id") + "|" + resultSet.getLong("nb_of_times")));
            }
            resultSet = selectTraceabilityDetailsTraceabilityAggregatePreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("traceability_details_id"), resultSet.getString("traceability_aggregate_id")));
            }
        }
        assertThat(data).containsExactly("1|2026-09-06 14:00:00+02|shouldStoreAndRetrieveTrace",
                "1|EU:4714636ab5e7b6ec200c9a0ec8a1b08f61df989c47f22f9e9322adf63922d9e4|EU:aliceEncoded",
                "1|U000001-T000001|1|1",
                "1|1");
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

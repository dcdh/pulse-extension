package com.damdamdeo.pulse.extension.it.api.todo;

import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.TodoRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class TodoEndpointTest {

    @Inject
    TodoRepository todoRepository;

    @Inject
    DataSource dataSource;

    @BeforeEach
    @AfterEach
    void teardown() throws SQLException {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("DELETE FROM todo_projection")) {
            statement.executeUpdate();
        }
    }

    @Test
    void shouldReturnOnlyDoneTodos() {
        // Given
        final TodoProjection done = new TodoProjection(ProjectionId.from(TodoId.USER_1_TODO_1),
                new Description("Todo terminé"), Status.DONE, true, List.of(), new Owner(UserId.USER_2));
        final TodoProjection inProgress = new TodoProjection(ProjectionId.from(TodoId.USER_1_TODO_2),
                new Description("Todo en cours"), Status.IN_PROGRESS, false, List.of(), Owner.USER_1);
        todoRepository.store(done);
        todoRepository.store(inProgress);

        // When
        final List<TodoDTO> response = given()
                .when().get("/todos/done")
                .then().statusCode(200)
                .extract().as(new TypeRef<List<TodoDTO>>() {});

        // Then
        assertThat(response).containsExactly(new TodoDTO(done));
    }

    @ApplicationScoped
    @Alternative
    @Priority(1)
    public static class AuthorizedJobExecutionContextProvider implements ExecutionContextProvider {

        @Override
        public ExecutionContext provide() {
            return new ExecutionContext(new ExecutedBy.Job(new JobName("ArchiveTodoSimpleScheduledJob")), Set.of());
        }
    }
}

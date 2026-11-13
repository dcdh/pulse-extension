package com.damdamdeo.pulse.extension.it.api.todo;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.job.JobName;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

@QuarkusTest
class TodoEndpointTest {

    @Test
    void shouldCreateTodo() {
        given()
                .contentType(ContentType.URLENC)
                .formParam("description", "Nouveau todo")
                .formParam("ownerId", "U000001")
                .when().post("/todos")
                .then().statusCode(201)
                .body("todoId", matchesPattern("U000001-T[0-9]{6}"))
                .body("description", equalTo("Nouveau todo"))
                .body("status", equalTo("IN_PROGRESS"))
                .body("important", equalTo(false))
                .body("ownerId", equalTo("U000001"));
    }

    @ApplicationScoped
    @Alternative
    @Priority(1)
    public static class JobExecutionContextProvider implements ExecutionContextProvider {

        @Override
        public ExecutionContext provide() {
            return new ExecutionContext(new ExecutedBy.Job(new JobName("CreateTodoTest")), Set.of());
        }
    }
}

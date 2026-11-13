---
name: writer-module
description: Work on the Pulse Writer Quarkus extension, including JDBC persistence, transactions, distributed locks, serialization, and build-time processors.
---

# Writer module

Use this skill for changes under `writer/runtime`, `writer/deployment`, or `writer/integration-tests`.

## Module layout

- `writer/runtime` contains runtime services and adapters for the core ports. Keep application behavior in the core module; the writer supplies persistence and Quarkus infrastructure.
- `writer/deployment` contains Quarkus build-time processors, validation, bean registration, and generated-code integration. Follow the existing `@BuildStep` and build-item patterns there.
- `writer/integration-tests` assembles the Writer extension with the core Todo fixtures and REST endpoints; it depends on the Writer runtime artifact.
- Tests live beside each module under `src/test`. Deployment tests exercise augmentation and generated beans; runtime tests cover focused runtime behavior.

## Persistence invariants

- Preserve the `EventRepository` contract: store events in version order and update the aggregate snapshot consistently with the last stored event. Empty event batches are a no-op.
- Keep SQL values parameterized. Follow the existing `JdbcPostgresEventRepository` encryption and ownership handling; do not add plaintext payload or identity storage.
- Translate infrastructure failures to the core exception expected by the port instead of leaking JDBC or serialization exceptions across the boundary.
- PostgreSQL session advisory locks must be acquired, used, and released on the same checked-out connection. Keep release in a `finally` block, including when use-case execution fails.
- Keep transaction propagation behavior in `DefaultQuarkusTransaction` aligned with the core `Transaction` contract, including unwrapping `CommandException` causes.

## Working on extension processors

- Inspect nearby processors and their tests before changing bean discovery, validation, or code generation. Respect capability checks and conditional registration so optional Quarkus integrations remain optional.
- Add or update deployment tests for observable augmentation behavior, such as generated beans, validation failures, and extension wiring. Add runtime tests for adapter behavior.
- Check both the affected Maven module and its related module when a change crosses the runtime/deployment boundary.

## Job writer reference

`job/runtime/.../batch/UseCaseWriter.java` is a caller-side batch adapter: it forwards commands to `DomainUseCase` and manages the job execution context. Use it to understand how jobs enter the core use-case API; its implementation belongs to the `job` module. Persistence behavior for those use cases belongs in `writer` through the core ports.

## REST integration-test examples

The following code comes from the Writer integration-test module and its core fixtures. Keep `Description` as a value object in the command and REST parameter. JAX-RS converts `@FormParam("description")` through the record's public `Description(String)` constructor; no `ParamConverter` or Jackson conversion is needed. The form request uses `application/x-www-form-urlencoded`, while the response remains JSON with status `201`.

The aggregate emits the existing string-valued `NewTodoCreated` event using `createTodo.description().description()`, preserving the persisted event shape. Application consumers keep their own standard Maven layout or `infrastructure` module; the extension's runtime/deployment structure is specific to this repository.

### Command with a value object

From `core/src/test/java/com/damdamdeo/pulse/extension/core/command/CreateTodo.java`:

```java
import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record CreateTodo(Description description, Owner owner) implements CreationalCommand<TodoId> {

    public CreateTodo {
        Objects.requireNonNull(description);
        Objects.requireNonNull(owner);
    }
}
```

`Description` provides the public `String` constructor used by REST parameter conversion:

```java
import java.util.Objects;

public record Description(String description) {

    public Description {
        Objects.requireNonNull(description);
    }
}
```

### REST endpoint and response DTO

From `writer/integration-tests/src/test/java/com/damdamdeo/pulse/extension/it/api/todo/TodoEndpoint.java`:

```java
import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.usecase.CreateTodoDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Objects;

@Path("/todos")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TodoEndpoint {

    private final CreateTodoDomainUseCase createTodoDomainUseCase;

    public TodoEndpoint(final CreateTodoDomainUseCase createTodoDomainUseCase) {
        this.createTodoDomainUseCase = Objects.requireNonNull(createTodoDomainUseCase);
    }

    @POST
    public Response createTodo(@FormParam("description") final Description description,
                               @FormParam("ownerId") final String ownerId) throws UseCaseException {
        final CreateTodo command = new CreateTodo(description, new Owner(UserId.from(ownerId)));
        final Todo todo = createTodoDomainUseCase.execute(command).aggregateRoot();
        return Response.status(Response.Status.CREATED).entity(new TodoDTO(todo)).build();
    }

    public record TodoDTO(String todoId, String description, Status status, boolean important, String ownerId) {

        public TodoDTO(final Todo todo) {
            this(todo.todoId().id(), todo.description().description(), todo.status(), todo.important(),
                    todo.owner().id().id());
        }
    }
}
```

### Form-based HTTP test

From `writer/integration-tests/src/test/java/com/damdamdeo/pulse/extension/it/api/todo/TodoEndpointTest.java`. The alternative supplies the test execution context while the real use case and persistence remain active:

```java
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
```

### Test configuration

From `writer/integration-tests/src/test/resources/application.properties`. The key is for tests only, and indexing and exclusions target this repository's fixtures:

```properties
quarkus.application.name=TodoWriter

quarkus.compose.devservices.enabled=true

quarkus.index-dependency.pulse-core-tests.group-id=com.damdamdeo.pulse
quarkus.index-dependency.pulse-core-tests.artifact-id=pulse-core
quarkus.index-dependency.pulse-core-tests.classifier=tests

quarkus.arc.exclude-types=com.damdamdeo.pulse.extension.core.usecase.GuardDomainUseCaseTest$StubDomainUseCase

pulse.encryption-storage.master-key=0123456789abcdef0123456789abcdef
```

This HTTP test verifies command creation and its response. For an application using Pulse Query, also verify the resulting projection through its REST endpoint as described below.

Run the Writer HTTP test from the reactor root:

```bash
mvn -pl writer/integration-tests -am -Dtest=TodoEndpointTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The core fixtures also discover projection listeners requiring `TodoRepository`. `writer/integration-tests/src/test/java/com/damdamdeo/pulse/extension/it/query/todo/InMemoryTodoRepository.java` supplies this fixture dependency; it does not provide durable query persistence. For command REST → projection REST end-to-end tests, use the application's real query adapter and poll with a bounded Awaitility timeout if updates are asynchronous. Do not seed the expected projection directly or mock command handling and persistence in those tests.

Declare Java imports and use simple type names except for name collisions. Leave a blank line between the class declaration and its first field, and between field declarations.

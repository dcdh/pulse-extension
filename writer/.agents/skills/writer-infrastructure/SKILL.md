---
name: writer-infrastructure
description: Integrate the Pulse Writer extension into a classic Quarkus application, including infrastructure wiring, REST command endpoints, persistence configuration, and end-to-end command-to-projection tests. Use writer-module for changes to the extension itself.
---

# Writer application infrastructure

Use this skill when an application consumes `com.damdamdeo.pulse:quarkus-pulse-writer-extension`. Follow the application's existing architecture and inspect nearby endpoints, use cases, configuration, and tests before adding infrastructure.

## Application layout and dependencies

A classic application has no separate `runtime` and `deployment` modules. It is either a single-module Maven project or a multi-module project whose Quarkus application lives in an `infrastructure` module. The Writer extension's own `writer/runtime` and `writer/deployment` directories describe extension implementation, not an application structure to reproduce.

- Add the Writer runtime dependency to the application module's `pom.xml`, using its existing version management. Quarkus resolves the extension's deployment side; do not add a direct deployment dependency or copy build-time processors into the application.
- Keep domain commands, events, aggregate roots, and domain use cases in the existing domain boundary. Put REST endpoints, DTOs, configuration, and infrastructure adapters in their existing application packages under `src/main/java`.
- Put application configuration in `src/main/resources/application.properties`, application-owned Flyway migrations in `src/main/resources/db/migration`, and tests and test configuration under `src/test` of the application module.
- Ensure separately packaged domain classes are indexed for Quarkus discovery. Follow existing Jandex setup or add `quarkus.index-dependency.<name>.group-id` and `.artifact-id` entries for the actual dependency when needed. Do not copy the extension tests' `pulse-core` test-jar indexing into an application.

## REST commands and extension wiring

- Map REST parameters or request DTOs to domain command records. For form-based command endpoints, use `@Consumes(MediaType.APPLICATION_FORM_URLENCODED)` and `@FormParam` with domain value-object types where conversion is supported. Inject the concrete domain use case and invoke its `execute(command)` method, then map the result to the application's response DTO and HTTP contract.
- Keep command validation and domain transitions in the domain. Endpoints must not directly change aggregate state or write events, snapshots, or projections to implement a command.
- Use the supported domain use-case base classes, including `AbstractCreationalDomainUseCase` and `AbstractDomainUseCase`, as appropriate. Writer validates use-case shapes and discovers their beans and generated guards. Do not introduce a direct implementation of `DomainUseCase` or redundant producers to work around discovery problems.
- Preserve generated permissions, execution context, transaction handling, and distributed locking by entering through the injected use case. Configure the application's real authentication and execution-context providers as required by its permissions.
- Let Writer provide its event repository, sequence generation, transaction adapter, and distributed lock implementation. Add application adapters only for application-specific ports. Preserve extension exception mapping and the application's REST error contract.
- Inspect existing datasource, encryption, and migration configuration before changing it. Keep secrets out of source-controlled production configuration and preserve encrypted event and identity storage.

## Form command endpoint example

Read [the Todo REST example](references/todo-rest-example.md) when implementing or testing a Writer command endpoint. It follows the actual `writer/integration-tests` code: a `Description` value object in `CreateTodo`, automatic form-parameter conversion through its public `String` constructor, constructor injection of the use case, and a JSON response with status `201`. Keep the value object through the REST-to-command boundary rather than converting it back to a string. The existing `NewTodoCreated` event still carries a string; the aggregate extracts it when emitting the event, preserving its persisted shape.

A `ParamConverter` is unnecessary for `Description`: its record constructor already accepts a single string. This is REST parameter conversion, independent of Jackson JSON mapping. Add a converter only when a type needs conversion that its supported constructor or factory methods do not provide. The current form endpoint has no JSON request DTO; `TodoDTO` remains the response contract.

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

## Application tests

Use plain JUnit for DTO conversion and isolated mapping behavior. Use `@QuarkusTest` for the assembled application, with the actual production endpoints, use cases, and persistence beans. Add test-scoped Quarkus JUnit, RestAssured, and Awaitility dependencies as needed, following the application's Quarkus dependency management. Extension deployment tests use internal `QuarkusUnitTest` archives to test augmentation; an application normally tests its production beans directly with `@QuarkusTest`.

Include end-to-end tests that start with a command sent to a REST endpoint and verify its effects through a projection returned by a query REST endpoint when the application uses Pulse Query:

1. Prepare isolated test data and an authenticated or authorized execution context for the command and query endpoints.
2. Send the command request with RestAssured and assert the expected HTTP status and response, retaining the created identifier when relevant.
3. Read the corresponding projection through its REST endpoint. Assert its identifier and expected business values, rather than only the HTTP status. For collection responses, deserialize with `TypeRef<List<ApplicationDTO>>` and compare the expected DTOs.
4. If projection updates are asynchronous, poll the query endpoint with Awaitility and a bounded timeout until those assertions pass. Do not use a fixed sleep.

Exercise real command handling, event persistence, projection updates, and query persistence. Do not mock these components or seed the expected projection directly in this end-to-end test. Focused repository-seeded query tests and domain unit tests complement this flow. When Query is not present, verify the Writer command's persisted result through the application's available observable behavior; do not add Query solely for a test.

Use PostgreSQL Dev Services with Docker accessible. Apply application migrations before seeding or cleanup. Keep test-only keys and configuration in `src/test/resources/application.properties`. Clean the relevant command-side and query-side state between tests using the application's existing lifecycle, accounting for asynchronous work. Add failure or authorization cases when relevant to the endpoint contract and verify that rejected commands do not produce the expected successful state change.

Run the affected application tests, replacing the example class name:

```bash
# Single-module application
mvn -Dtest=ApplicationCommandEndpointTest test

# From a multi-module application's reactor root; adapt the module path
mvn -pl infrastructure -am -Dtest=ApplicationCommandEndpointTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## Repository examples

These paths are references inside the Pulse extension repository, not application destinations:

- `writer/integration-tests/src/test/java/com/damdamdeo/pulse/extension/it/api/todo/TodoEndpoint.java` and `TodoEndpointTest.java` are the primary form-based creation endpoint and HTTP test examples. Their supporting configuration and limitations are explained in the linked Todo REST example.
- `writer/deployment/src/test/java/com/damdamdeo/pulse/extension/writer/deployment/UserRegistrationUpdateLifecycleCaseTest.java` demonstrates REST endpoints injecting use cases, executing commands, and mapping `Handled` results to DTOs. Adapt the behavior rather than copying its internal extension test harness or ordered tests.
- `writer/deployment/src/test/java/com/damdamdeo/pulse/extension/writer/deployment/domainusecase/DomainUseCaseTest.java` exercises real use-case wiring and permissions.
- `query/integration-tests/src/test/java/com/damdamdeo/pulse/extension/it/api/todo/TodoEndpointTest.java` demonstrates REST projection assertions, repository access, cleanup, and an authorized test context. Its repository-seeded setup is a focused query test; replace that setup with the command REST call for the end-to-end flow.
- `query/integration-tests/src/test/java/com/damdamdeo/pulse/extension/it/query/todo/TodoObjectMapperCustomizerQuarkusTest.java` verifies production Jackson wiring, projection round trips, and absence of sensitive plaintext.

## Java style

Declare imports for the types used and use their simple names, except when a name collision requires qualification. Leave a blank line between the class declaration and its first field, and between field declarations.

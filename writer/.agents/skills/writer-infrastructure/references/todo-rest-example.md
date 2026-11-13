# Todo creation through a REST form

## Domain value object and command

The actual types are in `core/src/test/java/com/damdamdeo/pulse/extension/core/Description.java` and `command/CreateTodo.java`:

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

`Description` is a record with a public constructor accepting `String`, and rejects a null value:

```java
public record Description(String description) {

    public Description {
        Objects.requireNonNull(description);
    }
}
```

Use `new CreateTodo(new Description("lorem ipsum"), Owner.USER_1)` at Java call sites. When changing a command's component type, update its consumers in core, Writer, jobs, and workflows. In `TodoAggregateRoot.handle(CreateTodo, ...)`, the existing string-valued event is constructed as follows:

```java
eventAppender.append(new NewTodoCreated(createTodo.description().description(), createTodo.owner()));
```

The command's value-object type does not require changing the persisted event schema.

## Form endpoint

`writer/integration-tests/src/test/java/com/damdamdeo/pulse/extension/it/api/todo/TodoEndpoint.java` uses these annotations and constructor injection. The relevant code is:

```java
import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
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

    // See the source file for the nested TodoDTO record.
}
```

The form sends `description=Nouveau todo` and `ownerId=U000001`. Quarkus REST converts the description string using `Description(String)`; no `ParamConverter` or Jackson customizer is needed for this parameter. The owner ID is converted explicitly with `UserId.from(ownerId)` and wrapped in `Owner`. Keep the generated use-case guards and transaction handling active by calling the injected use case.

`TodoDTO` is constructed from the shared `Todo` interface. It returns `todoId`, a string `description`, `status`, `important`, and `ownerId`. The form request and JSON response are separate contracts; accepting a value object does not force the response to expose its nested record structure.

## HTTP test and fixture configuration

`writer/integration-tests/src/test/java/com/damdamdeo/pulse/extension/it/api/todo/TodoEndpointTest.java` uses `@QuarkusTest` and the following RestAssured request and assertions:

```java
import io.restassured.http.ContentType;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

// Inside shouldCreateTodo():
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
```

This checks form conversion, real command execution, and the creation response. A test-only `@ApplicationScoped`, `@Alternative`, `@Priority(1)` implementation of `ExecutionContextProvider` supplies `new ExecutionContext(new ExecutedBy.Job(new JobName("CreateTodoTest")), Set.of())`. Adapt the identity to the use case's permissions; do not remove guards to make the test pass.

Supporting files in `writer/integration-tests`:

- `pom.xml` belongs to `quarkus-pulse-writer-extension-parent` and depends on the Writer runtime artifact. It includes the core test-jar, Quarkus JUnit, and RestAssured. Use the application's dependency management when adapting it.
- `src/test/resources/application.properties` sets the application name to `TodoWriter`, enables Compose Dev Services, indexes the `pulse-core` test-jar, excludes the unrelated `GuardDomainUseCaseTest$StubDomainUseCase`, and supplies a test-only `pulse.encryption-storage.master-key`. Real encrypted event persistence needs this key. Do not copy fixture-specific indexing or exclusions into production configuration.
- `src/test/java/com/damdamdeo/pulse/extension/it/query/todo/InMemoryTodoRepository.java` supports projection listeners discovered in the core fixtures. This in-memory fixture does not demonstrate durable query persistence or a REST query endpoint. For an application with Pulse Query, additionally test the full command REST → projection REST flow with its real query adapter, as required in the main skill.
- `.gitignore` excludes the module's `target` directory and generated Compose/SQL files. Let the extension generate those files rather than hand-maintaining its database schema.

From the Pulse extension reactor root, run:

```bash
mvn -pl writer/integration-tests -am -Dtest=TodoEndpointTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

If Quarkus bootstrap cannot resolve a newly created module artifact, install the reactor dependencies and module locally with `mvn -pl writer/integration-tests -am -DskipTests install`, then retry the test. Inspect the deepest cause first; this recovery addresses missing local artifacts, not CDI or database failures. PostgreSQL Dev Services requires Docker access.

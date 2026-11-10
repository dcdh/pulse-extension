---
name: domain-testing
description: Explain and create tests for domain aggregates and command-handler behavior in Pulse, using emitted events and resulting aggregate state as the main observable outcomes.
---

# Testing the Pulse domain

Use this skill when adding or changing tests for aggregates, domain commands, event application, or command handling. Domain examples and the handler test fixture currently live in `core/src/test/java`.

## Start with the behavior

- Read the aggregate and its command/event records first. `core/src/test/java/com/damdamdeo/pulse/extension/core/Todo.java` demonstrates command `handle(...)` methods emitting events and `on(...)` methods applying them to aggregate state.
- Read `core/src/test/java/com/damdamdeo/pulse/extension/core/command/TodoCommandHandlerTest.java` for command-handler scenarios, especially how creation, replay, event versions, persistence, and failures are asserted.
- Name each test with the `should...` form, describing the expected behavior (for example, `shouldMarkTodoAsDone` or `shouldThrowCommandExceptionWhenTodoIsMissing`). Keep the name focused on one behavior.
- Structure every test with explicit `// Given`, `// When`, and `// Then` sections. Set up only the scenario in Given, perform the action under test in When, and put outcome assertions in Then.
- For command-handler tests that use Mockito, annotate the test class with `@ExtendWith(MockitoExtension.class)` and declare mocked ports with `@Mock`. Prefer real domain objects and the real command handler; mock external collaborators such as repositories and ID generators.
- Use static imports for `assertAll`, `assertThat`, and Mockito methods such as `doReturn` and `verify`. Put state, event, and mock interaction checks inside the same `assertAll(...)` in Then; write each `verify(...)` as its own lambda alongside the other assertions. This reports all failed checks together while keeping mock verifications at the end of the Then section.

## Test aggregates directly

When testing domain rules without the command-handler pipeline:

- Construct the aggregate from its identifier and replay any initial events through the same event application path used by production code.
- Invoke the command handler on the aggregate with a real execution context and an event appender or a small capturing implementation.
- Assert the events emitted by the command, and separately assert the state produced when those events are applied. Cover invalid transitions by asserting the business exception and its meaningful cause or message.
- Avoid mocking the aggregate or its value objects; the rule under test belongs to the domain model.

For example, `Todo.handle(MarkTodoAsDone, ...)` checks the current state and appends the domain event only when the transition is allowed:

```java
public void handle(final MarkTodoAsDone command,
                   final ExecutionContext executionContext,
                   final EventAppender<TodoId> eventAppender) throws BusinessException {
    Objects.requireNonNull(command);
    Objects.requireNonNull(eventAppender);
    Objects.requireNonNull(executionContext);
    TodoChecker.TODO_IN_PROGRESS.check(this, executionContext);
    eventAppender.append(new TodoMarkedAsDone());
}
```

This example is from `core/src/test/java/com/damdamdeo/pulse/extension/core/Todo.java`; use the aggregate's real command and event types rather than replacing them with mocks.

## Test the command-handler pipeline

Use `TodoCommandHandlerTest` as the primary pattern when verifying orchestration across the aggregate and its ports:

- Mock `EventRepository` and `AggregateIdGenerator`; use the real command handler, registry, aggregate, and transaction test double. The example uses `NotAvailableExecutionContextProvider` as a spy so it can assert execution-context lookups.
- Leave one blank line between the test class declaration and its first property or field, matching `TodoCommandHandlerTest`.
- For a creational command, arrange the generated identifier and `hasEventsFor(...) == false`, call the creation overload, then assert the returned identifier, aggregate state, and complete emitted event sequence starting at version zero.
- For a command against an existing aggregate, arrange repository history with `loadOrderByVersionASC(...)`, call the existing-aggregate overload, and assert the new event version follows the replayed history. Check the updated aggregate state as well as `Handled.events()`.
- Assert `ExecutedByEvent` wrapping when execution identity is part of the behavior. Verify that `EventRepository.save(...)` receives the exact newly emitted versioned events, resulting aggregate root, and execution identity.
- Keep failure scenarios explicit: business failures become `CommandException` with the original cause, missing aggregates use the supplied exception, duplicates are checked during creation, and unhandled commands/events exercise the documented unsupported-operation behavior.
- Prefer exact event assertions (`containsExactly`) when ordering and version numbers are part of the contract. Avoid asserting incidental implementation calls unless they protect a domain or persistence contract.

The following excerpt follows `shouldMarkTodoAsDone` in `core/src/test/java/com/damdamdeo/pulse/extension/core/command/TodoCommandHandlerTest.java`. The fixture provides the handler and mocked repository:

```java
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TodoCommandHandlerTest {

    @Mock
    EventRepository<Todo, TodoId> todoEventRepository;

    // Other fixture setup and tests...
}
```

Within that fixture, a test follows this Given / When / Then pattern. Keep mock verifications at the end of Then, after checking the returned aggregate and emitted events:

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@Test
void shouldMarkTodoAsDone() throws CommandException {
    // Given
    final MarkTodoAsDone command = new MarkTodoAsDone(TodoId.USER_1_TODO_1);
    doReturn(List.of(new ExecutedByEvent<>(
            new NewTodoCreated("lorem ipsum", Owner.USER_1),
            ExecutedBy.NotAvailable.INSTANCE)))
            .when(todoEventRepository).loadOrderByVersionASC(TodoId.USER_1_TODO_1);

    // When
    final Handled<Todo, TodoId> handled = todoCommandHandler.handle(
            command, () -> new UnknownTodoException(TodoId.USER_1_TODO_1));

    // Then
    assertAll(
            () -> assertThat(handled.id()).isEqualTo(TodoId.USER_1_TODO_1),
            () -> assertThat(handled.aggregateRoot().status()).isEqualTo(Status.DONE),
            () -> assertThat(handled.events()).containsExactly(new VersionizedEvent<>(
                    new AggregateVersion(1),
                    new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE))),
            () -> verify(todoEventRepository).save(
                    handled.events(), handled.aggregateRoot(), ExecutedBy.NotAvailable.INSTANCE)
    );
}
```

Show the static imports used by the example at the top of the snippet. Keep each Then check in its own lambda so JUnit reports all failed checks in one run.

## Useful test commands

Run the focused test while iterating, then the core module tests when the change affects shared domain behavior:

```bash
mvn -pl core -Dtest=TodoCommandHandlerTest test
mvn -pl core test
```

If a test crosses into a Quarkus extension adapter, also run that adapter module's focused test. Keep domain rule assertions in `core`; test infrastructure wiring in its owning module.

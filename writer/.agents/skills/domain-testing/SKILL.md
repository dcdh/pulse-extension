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
- In Then, group related assertions with `org.junit.jupiter.api.Assertions.assertAll(...)` so all relevant outcomes are reported together. Use one lambda per assertion; include returned state, emitted events, and important port interactions as appropriate.

## Test aggregates directly

When testing domain rules without the command-handler pipeline:

- Construct the aggregate from its identifier and replay any initial events through the same event application path used by production code.
- Invoke the command handler on the aggregate with a real execution context and an event appender or a small capturing implementation.
- Assert the events emitted by the command, and separately assert the state produced when those events are applied. Cover invalid transitions by asserting the business exception and its meaningful cause or message.
- Avoid mocking the aggregate or its value objects; the rule under test belongs to the domain model.

## Test the command-handler pipeline

Use `TodoCommandHandlerTest` as the primary pattern when verifying orchestration across the aggregate and its ports:

- Mock `EventRepository` and `AggregateIdGenerator`; use the real command handler, registry, aggregate, and transaction test double. The example uses `NotAvailableExecutionContextProvider` as a spy so it can assert execution-context lookups.
- For a creational command, arrange the generated identifier and `hasEventsFor(...) == false`, call the creation overload, then assert the returned identifier, aggregate state, and complete emitted event sequence starting at version zero.
- For a command against an existing aggregate, arrange repository history with `loadOrderByVersionASC(...)`, call the existing-aggregate overload, and assert the new event version follows the replayed history. Check the updated aggregate state as well as `Handled.events()`.
- Assert `ExecutedByEvent` wrapping when execution identity is part of the behavior. Verify that `EventRepository.save(...)` receives the exact newly emitted versioned events, resulting aggregate root, and execution identity.
- Keep failure scenarios explicit: business failures become `CommandException` with the original cause, missing aggregates use the supplied exception, duplicates are checked during creation, and unhandled commands/events exercise the documented unsupported-operation behavior.
- Prefer exact event assertions (`containsExactly`) when ordering and version numbers are part of the contract. Avoid asserting incidental implementation calls unless they protect a domain or persistence contract.

## Useful test commands

Run the focused test while iterating, then the core module tests when the change affects shared domain behavior:

```bash
mvn -pl core -Dtest=TodoCommandHandlerTest test
mvn -pl core test
```

If a test crosses into a Quarkus extension adapter, also run that adapter module's focused test. Keep domain rule assertions in `core`; test infrastructure wiring in its owning module.

---
name: domain-aggregate
description: Define or change Pulse domain aggregate roots, including their identifiers, commands, events, state transitions, and ownership relationships.
---

# Defining a Pulse domain aggregate root

Use this skill when creating or changing a domain aggregate, its aggregate ID, commands, events, or ownership rules. Keep domain behavior in `core`; the `writer` module provides persistence and Quarkus integration rather than domain rules.

## Read the local model first

- Use `core/src/test/java/com/damdamdeo/pulse/extension/core/Todo.java` as the main example of command handling, event application, state accessors, and ownership.
- Use `core/src/test/java/com/damdamdeo/pulse/extension/core/TodoChecklist.java` for a child aggregate whose `BelongsTo` and ID link to a parent aggregate.
- Inspect related command and event records before choosing a new event shape. Keep naming, ID formatting, and business exception patterns consistent with neighboring aggregates.

## Aggregate structure

- Define an ID type implementing `AggregateId` and make it a Java record. Its `id()` must be stable and unique; if it is a child ID, retain the parent ID needed to derive `belongsTo()` and `ownedBy()`.
- Define commands and domain events as records. The Writer deployment validation requires aggregate IDs, commands, and events to be records.
- Extend `AggregateRoot<IdType>` and provide both required constructors: one taking only the ID, which passes it to `super(id)` and supports event replay, and one taking the ID plus every aggregate state property, which initializes the complete state. Do not omit either constructor.
- Use the ID-only constructor when the command handler creates an empty aggregate to replay history. Use the full-state constructor when a caller already has the complete aggregate state. The current `ReflectionAggregateRootInstanceCreator` uses the ID-only constructor for replay.
- Implement `belongsTo()` and `ownedBy()` from the domain's actual relationship and ownership rules. Do not guess these values from request context when the aggregate ID or state already defines them.
- Keep state private and expose only the accessors needed by domain behavior and consumers. Use explicit null checks for required constructor, command, event, and execution-context inputs, following nearby code.

## Shared domain contract for query mapping

- Define a small domain interface for the aggregate's query-facing properties when the query side needs to map aggregate data into a DTO. Put the interface with the domain model in `core`; expose only the values required by the mapping.
- Have the aggregate root implement that interface and return its state through the interface accessors. The query DTO/projection should implement the same interface when it represents the same data shape. This lets mapping code depend on the shared contract instead of the concrete aggregate class.
- Keep the interface as a read contract: do not put command handling, event application, persistence, or query-specific behavior on it.

`Todo` is the example contract: `TodoAggregateRoot` implements it, and the query-side `TodoProjection` also implements it so the same accessors can be used for DTO mapping.

```java
public interface Todo {

    TodoId todoId();

    Description description();

    Status status();

    boolean important();

    boolean isInProgress();

    Owner owner();
}
```

```java
public final class TodoAggregateRoot extends AggregateRoot<TodoId> implements Todo {

    // Aggregate state and command/event behavior...

    @Override
    public TodoId todoId() {
        return id;
    }

    @Override
    public Description description() {
        return description;
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public boolean important() {
        return important;
    }

    @Override
    public boolean isInProgress() {
        return Status.IN_PROGRESS.equals(status);
    }

    @Override
    public Owner owner() {
        return owner;
    }
}
```

The query-side example is `core/src/test/java/com/damdamdeo/pulse/extension/core/query/TodoProjection.java`; check its current shape and the corresponding DTO mapper before adding or changing shared accessors.

## Commands and events

- Implement each command transition in `handle(Command, ExecutionContext, EventAppender<Id>)`. Validate business rules against current aggregate state and append domain events through the provided appender instead of directly changing state.
- Implement `on(Event, ExecutedBy)` methods to apply events to aggregate state. Keep event application deterministic and free of persistence, network calls, or other infrastructure work.
- Represent business rejection with `BusinessException` and a meaningful cause. Do not use `on(...)` as the command entry point; `CommandHandler` invokes `handle(...)` and applies replayed and newly emitted events through `on(...)`.
- Use events to represent durable state changes. When one command produces several events, preserve their meaningful order and ensure replaying them reconstructs the same aggregate state.

Example shape, adapted from `Todo`:

```java
public final class Item extends AggregateRoot<ItemId> {

    private String name;

    public Item(final ItemId id) {
        super(id);
    }

    public Item(final ItemId id, final String name) {
        super(id);
        this.name = Objects.requireNonNull(name);
    }

    public void handle(final RenameItem command,
                       final ExecutionContext executionContext,
                       final EventAppender<ItemId> eventAppender) throws BusinessException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(executionContext);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new ItemRenamed(command.name()));
    }

    public void on(final ItemRenamed event, final ExecutedBy executedBy) {
        Objects.requireNonNull(event);
        Objects.requireNonNull(executedBy);
        name = event.name();
    }

    @Override
    public BelongsTo belongsTo() {
        return BelongsTo.from(id.userId());
    }

    @Override
    public OwnedBy ownedBy() {
        return OwnedBy.from(id.userId());
    }
}
```

The names above illustrate structure; use the real ID relationship and ownership rules for the domain being implemented.

## Stored-event listeners and domain projections

For each domain event that creates or changes query-side state, implement a domain listener extending `AbstractOnStoredEventListener<AggregateIdType, EventType>`. Cover the different events affecting the projection, such as creation, status changes, and archival, so the read model reflects those transitions. These listeners complement the aggregate's `on(Event, ExecutedBy)` methods: the aggregate applies events to its own state, while stored-event listeners maintain projections after event storage.

- Keep each listener implementation in the domain, alongside its events and query model. Implement `eventType()` with the concrete event class and `on(id, aggregateVersion, event)` with the projection update. The base class filters events by type; do not override its final `execute(...)` method.
- Each listener depends on a repository **interface defined in the domain**, supplied through its constructor. Its methods accept and return domain projections, never infrastructure DTOs, JDBC types, or persistence entities. Implement this repository interface in the application's infrastructure layer or `infrastructure` module, using the domain projection in the adapter's method signatures. The domain listener depends only on the interface; infrastructure supplies its concrete persistence implementation.
- Define the projection in the domain and implement `Projection<AggregateIdType>`, using domain IDs and value objects. Both the listener and repository interface refer to that same projection. Keep the actual repository adapter and storage configuration in infrastructure.
- For creation events, construct and store a projection. For subsequent events, load it through the domain repository, derive the updated projection, and store it. For example, `TodoMarkedAsDoneEventListener` loads with `getByTodoId(id)` and stores `projection.withStatus(Status.DONE)`.
- Let Writer discover the listener implementations. Keep domain listeners free of Quarkus/CDI and JDBC annotations; do not add manual listener producers in an application to duplicate extension registration.

The base class is `core/src/main/java/com/damdamdeo/pulse/extension/core/listener/AbstractOnStoredEventListener.java`. The following examples come directly from the domain fixtures in `core/src/test/java/com/damdamdeo/pulse/extension/core/query`.

### Domain repository interface

```java
import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Collection;

public interface TodoRepository {

    void store(TodoProjection todoProjection);

    TodoProjection getByTodoId(TodoId todoId);

    Collection<TodoProjection> findAll();
}
```

### Domain projection

```java
import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.List;
import java.util.Objects;

public record TodoProjection(ProjectionId<TodoId> projectionId,
                             Description description,
                             Status status,
                             boolean important,
                             List<TodoChecklistProjection> checklist,
                             Owner owner) implements Projection<TodoId>, Todo {

    public TodoProjection {
        Objects.requireNonNull(projectionId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(status);
        Objects.requireNonNull(checklist);
        Objects.requireNonNull(owner);
    }

    public TodoProjection withStatus(final Status status) {
        return new TodoProjection(projectionId, description, status, important, checklist, owner);
    }

    @Override
    public TodoId todoId() {
        return projectionId.linkTo();
    }

    @Override
    public boolean isInProgress() {
        return Status.IN_PROGRESS.equals(status);
    }

    @Override
    public ProjectionId<TodoId> id() {
        return projectionId;
    }
}
```

### Creation-event listener

```java
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.listener.AbstractOnStoredEventListener;
import com.damdamdeo.pulse.extension.core.listener.OnStoredEventListenerException;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.TodoRepository;

import java.util.List;
import java.util.Objects;

public class NewTodoCreatedEventListener extends AbstractOnStoredEventListener<TodoId, NewTodoCreated> {

    private final TodoRepository todoRepository;

    public NewTodoCreatedEventListener(final TodoRepository todoRepository) {
        this.todoRepository = Objects.requireNonNull(todoRepository);
    }

    @Override
    public void on(final TodoId id, final AggregateVersion aggregateVersion, final NewTodoCreated event) throws OnStoredEventListenerException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(aggregateVersion);
        Objects.requireNonNull(event);
        final TodoProjection todoProjection = new TodoProjection(ProjectionId.from(id), new Description(event.description()),
                Status.IN_PROGRESS, false, List.of(), event.owner());
        todoRepository.store(todoProjection);
    }

    @Override
    protected Class<NewTodoCreated> eventType() {
        return NewTodoCreated.class;
    }
}
```

Test listener behavior by passing the event through `execute(...)`, then assert the projection stored through the domain repository interface. For application integration tests, exercise the actual command and listener wiring and read the resulting projection through REST, following the application's Writer/Query test conventions.

## Integration and verification

- Define the command records, event records, and aggregate ID alongside the domain model in `core`; keep repository interfaces and projections in the domain, and repository implementations, SQL, serialization, and Quarkus annotations in their owning infrastructure modules.
- Connect the aggregate to the appropriate command handler/use case using existing core patterns. Check the Writer deployment processors and validation tests when adding record types or changing discovery behavior.
- Add domain tests for successful state transitions, emitted event order, replay, and rejected transitions. Follow the `domain-testing` skill for the project's Given / When / Then and Mockito test conventions.
- Run focused core tests first. Run Writer deployment tests when changing record validation or aggregate discovery.

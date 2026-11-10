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

## Integration and verification

- Define the command records, event records, and aggregate ID alongside the domain model in `core`; keep repositories, SQL, serialization, and Quarkus annotations in their owning infrastructure modules.
- Connect the aggregate to the appropriate command handler/use case using existing core patterns. Check the Writer deployment processors and validation tests when adding record types or changing discovery behavior.
- Add domain tests for successful state transitions, emitted event order, replay, and rejected transitions. Follow the `domain-testing` skill for the project's Given / When / Then and Mockito test conventions.
- Run focused core tests first. Run Writer deployment tests when changing record validation or aggregate discovery.

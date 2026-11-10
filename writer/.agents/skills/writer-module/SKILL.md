---
name: writer-module
description: Work on the Pulse Writer Quarkus extension, including JDBC persistence, transactions, distributed locks, serialization, and build-time processors.
---

# Writer module

Use this skill for changes under `writer/runtime` or `writer/deployment`.

## Module layout

- `writer/runtime` contains runtime services and adapters for the core ports. Keep application behavior in the core module; the writer supplies persistence and Quarkus infrastructure.
- `writer/deployment` contains Quarkus build-time processors, validation, bean registration, and generated-code integration. Follow the existing `@BuildStep` and build-item patterns there.
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

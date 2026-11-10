---
name: query-module
description: Work on the Pulse Query Quarkus extension, including query runtime adapters, projection validation, generated guards, caching, GDPR serialization, and file queries.
---

# Query module

Use this skill for changes under `query/runtime` or `query/deployment`.

## Module layout

- `query/runtime` contains runtime adapters and services for query-related core ports, including file persistence, owned-by lookup, caching, and serialization.
- `query/deployment` contains Quarkus build-time processors, generated query guards, database setup, and validation of application query models.
- Runtime tests cover focused service behavior. Deployment tests use Quarkus test support to verify augmentation, CDI wiring, endpoints, database integration, and validation failures.

## Query model and deployment rules

- Read nearby implementations and tests before changing query behavior. Keep domain query contracts in `core`; put Quarkus integration and infrastructure in this module.
- `ValidationProcessor` validates the value graph of a `Projection`: nested values must be projections or aggregate IDs, with primitives, enums, arrays, and collections handled by the processor. Update validation tests when expanding supported types.
- `CodeGenerationProcessor` generates guard query beans from discovered query use cases. Preserve generic type discovery, CDI qualifiers, and conditional behavior when changing generated guards.
- Keep deployment-time checks actionable. Add a Quarkus deployment test for invalid projection shapes or missing extension wiring rather than relying only on a unit test of a processor helper.

## Privacy, persistence, and cache behavior

- Read `query/ADR.adoc` before changing sensitive-value serialization or cache consistency. It documents the `@Sensitive` modes: encryption is non-deterministic; the optional hash supports equality searches and is deterministic.
- Do not serialize sensitive plaintext alongside its encrypted representation. Preserve the custom Jackson serializer/deserializer behavior for value objects such as `OwnedBy`.
- Use parameterized SQL in JDBC adapters and map database or serialization failures to the exception declared by the relevant core port. Keep JDBC resources closed on success and failure, including streaming file responses.
- Treat cache contents as derived data. Follow the version/count checks and fallback behavior documented by the ADR so stale or unavailable cache data does not silently return an incorrect result.
- When handling deletion or banning of an aggregate, account for corresponding query-side data cleanup as described by the ADR.

## Testing and validation

- Use `query/deployment/src/test/java` as the main reference for Quarkus-level behavior. File query, repository, endpoint, and generated guard tests are grouped by feature.
- Use the existing test resources under `query/deployment/src/test/resources` for PostgreSQL, OpenAPI, and file-processing cases. Keep tests focused on observable responses, stored data, privacy behavior, and augmentation errors.
- Run the narrowest affected test first, then the relevant module tests:

```bash
mvn -pl query/runtime test
mvn -pl query/deployment test
```

- If a change crosses runtime and deployment, verify both. If it changes a core query contract, also run the relevant `core` tests.

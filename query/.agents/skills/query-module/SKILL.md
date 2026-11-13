---
name: query-module
description: Work on the Pulse Query Quarkus extension or integrate it into an application, including query runtime adapters, projection validation, generated guards, caching, GDPR serialization, file queries, and query integration tests.
---

# Query module

Use this skill for changes to the Pulse Query extension or query features in applications that consume it. Determine which context applies before choosing paths or module boundaries.

## Extension repository layout

This layout describes the Pulse extension repository only. Do not reproduce it in an application that consumes the extension.

- `query/runtime` contains runtime adapters and services for query-related core ports, including file persistence, owned-by lookup, caching, and serialization.
- `query/deployment` contains Quarkus build-time processors, generated query guards, database setup, and validation of application query models.
- `query/integration-tests` exercises the assembled extension with a PostgreSQL-backed Todo projection, Jackson configuration, and REST endpoints under `src/test/java/com/damdamdeo/pulse/extension/it`.
- Runtime tests cover focused service behavior. Deployment tests use Quarkus test support to verify augmentation, CDI wiring, endpoints, database integration, and validation failures.

## Classic application projects

A classic application is a regular Quarkus application that consumes the Pulse Query extension, rather than implementing the extension itself. It has no separate `runtime` and `deployment` modules: those belong to the extension and are supplied by its dependencies. The application is either a single-module Maven project or, in a multi-module project, the Quarkus application lives in an `infrastructure` module. In the single-module case, application sources, resources, tests, and extension dependencies belong to that project's standard Maven layout and `pom.xml`. In the multi-module case, Quarkus wiring, adapters, configuration, and extension dependencies belong to the existing `infrastructure` module; preserve the project's domain and API boundaries.

Follow the application's existing packages and module boundaries. Put application projections and query use cases in its domain/query packages, JDBC repositories and Jackson customizers in its infrastructure packages, and endpoints and DTOs in its API packages, normally under `src/main/java`. Put Flyway migrations in `src/main/resources/db/migration`, application configuration in `src/main/resources/application.properties`, and tests and test-specific configuration under `src/test`. These are responsibilities, not mandatory modules or package names: do not create `query/runtime`, `query/deployment`, `query/integration-tests`, or a `core` module solely to match this repository. Depend on the Query runtime extension and let Quarkus load its deployment processors; application code should not copy those processors or register duplicate query-use-case producers. Index separate domain dependencies when needed for discovery. The integration-test fixtures below illustrate behavior to adapt into production application code, not files to copy into an application's test sources. Repository-relative paths and reactor test commands in this skill apply only when working inside the extension repository.

## Testing a classic application

Place application tests in `src/test/java` and test configuration in `src/test/resources` of the single Maven module or the Quarkus `infrastructure` module. Keep production endpoints, repositories, and Jackson customizers in `src/main`: the extension's integration-test fixture puts them in test sources to assemble a sample application, whereas an application tests its own production beans. No separate `deployment`, `runtime`, or `integration-tests` module is needed.

Use the examples under `query/integration-tests/src/test/java/com/damdamdeo/pulse/extension/it` as behavioral references, adapting Todo types, permissions, and SQL tables to the application's domain:

- `api/todo/TodoDTOTest.java`: use plain JUnit for DTO mapping and JSON contracts. Assert the expected JSON representation and the deserialized DTO, including value objects exposed as plain JSON values. Initialize the mapper outside the test method; configure only the customizers needed for this contract.
- `query/todo/TodoObjectMapperCustomizerTest.java`: isolate sensitive-value serialization with the real Jackson encryption module and mocked cryptographic collaborators. Verify encryption, decryption, complete projection round trips, and absence of sensitive plaintext. This focused test complements the assembled application test.
- `query/todo/TodoObjectMapperCustomizerQuarkusTest.java`: use `@QuarkusTest` and inject the application's `ObjectMapper` to check actual CDI registration and extension configuration. Round-trip a representative projection, including nested projections and typed IDs, and assert that sensitive fields are encrypted without leaking plaintext. Do not manually assemble the mapper or replace its cryptographic services in this test.
- `api/todo/TodoEndpointTest.java`: use `@QuarkusTest`, inject the real projection repository, store matching and non-matching projections, then call the endpoint with RestAssured. Check the HTTP status and deserialize the result with `TypeRef<List<ApplicationDTO>>`; compare the expected DTOs with one `containsExactly(...)`. Clean the projection table before and after each test using the injected `DataSource`. Supply an authorized execution context through a test-only CDI alternative when needed, while keeping generated query guards active.

### End-to-end tests with the Writer extension

When the application also depends on `quarkus-pulse-writer-extension`, include end-to-end tests covering the complete command-to-projection flow through the application's REST API. Start by calling a REST endpoint that dispatches a domain command, check its HTTP response, then call a query REST endpoint and assert that the returned projection reflects the command's effects. Use the real application wiring, command handling, event persistence, projection updates, and query repository. Provide the authentication or execution context required by both endpoints.

Do not seed the expected projection directly or mock the command handler, event processing, or query repository in these end-to-end tests: the command must produce the state observed through the query endpoint. Repository-seeded endpoint tests remain useful for focused query behavior, but do not replace this complete flow. Isolate test data and clean up the relevant command-side and query-side persisted state using the application's existing test lifecycle. If projection updates are asynchronous, poll the query REST endpoint with Awaitility until the expected projection is returned, using a bounded timeout rather than a fixed sleep. Assert the projection's expected values, not only a successful HTTP status.

Use PostgreSQL Dev Services for database-backed tests, with Docker accessible. Apply the application's Flyway migrations before cleanup or seeding; keep production migrations in `src/main/resources/db/migration`. In `src/test/resources/application.properties`, configure test-only settings such as a non-production `pulse.query.master-key` and dependency indexing when domain types live in a separate unindexed artifact. The fixture's `pulse-core` test-jar index and `baseline-version=0` solve fixture-specific discovery and migration needs; adapt them only when the application has the same need. See the persistence and discovery conventions below for details.

Keep the application's dependency on the Query runtime extension; Quarkus resolves its deployment side. Add test-scoped Quarkus JUnit, JUnit Jupiter, RestAssured, and Mockito only as needed, using the application's Quarkus version and dependency management. Include the PostgreSQL JDBC and Flyway dependencies when required by persistence. Do not copy the integration-test POM's direct deployment dependency, extension annotation processor, or internal Quarkus test tooling into an application merely to run these tests.

Run tests from the application repository, selecting its own test classes. For a single-module project:

```bash
mvn -Dtest=ApplicationDTOTest,ApplicationObjectMapperQuarkusTest,ApplicationEndpointTest test
```

For an application whose Quarkus module is `infrastructure`, run from its reactor root (adapt the module path):

```bash
mvn -pl infrastructure -am \
  -Dtest=ApplicationDTOTest,ApplicationObjectMapperQuarkusTest,ApplicationEndpointTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

These are example test names to replace with the application's classes. The examples use Surefire `*Test` classes annotated with `@QuarkusTest`; the name of the extension's `integration-tests` module does not require Failsafe or `@QuarkusIntegrationTest` in the application. Follow any existing application setup for packaged-application tests separately.

## Java imports

Declare Java imports for the types used in source files and use their simple names in code. Use fully qualified names only when needed to resolve a name collision.

Leave a blank line between the class declaration and its first field, and between field declarations.

## Query model and deployment rules

- Read nearby implementations and tests before changing query behavior. In the extension repository, keep domain query contracts in `core` and Quarkus integration in the Query module. In an application, preserve the equivalent separation within its existing architecture.
- `ValidationProcessor` validates the value graph of a `Projection`: nested values may be projections, aggregate IDs, or record value objects, with primitives, Java language types, enums, arrays, and collections handled by the processor. Validate record components recursively, including records inside collections and arrays. Track visited types per traversal branch so cycles terminate without hiding errors in sibling properties. Update validation tests when expanding supported types.
- `CodeGenerationProcessor` generates guard query beans from discovered query use cases. Preserve generic type discovery, CDI qualifiers, and conditional behavior when changing generated guards.
- Keep deployment-time checks actionable. Add a Quarkus deployment test for invalid projection shapes or missing extension wiring rather than relying only on a unit test of a processor helper.

## Privacy, persistence, and cache behavior

- Read `query/ADR.adoc` before changing sensitive-value serialization or cache consistency. It documents the `@Sensitive` modes: encryption is non-deterministic; the optional hash supports equality searches and is deterministic.
- For persisted projections, do not serialize sensitive plaintext alongside its encrypted representation. Preserve the custom Jackson serializer/deserializer behavior for value objects such as `OwnedBy`.
- Use parameterized SQL in JDBC adapters and map database or serialization failures to the exception declared by the relevant core port. Keep JDBC resources closed on success and failure, including streaming file responses.
- Treat cache contents as derived data. Follow the version/count checks and fallback behavior documented by the ADR so stale or unavailable cache data does not silently return an incorrect result.
- When handling deletion or banning of an aggregate, account for corresponding query-side data cleanup as described by the ADR.

## Integration-test conventions

### Persistence and Jackson

- Use `query/integration-tests/src/test/java/com/damdamdeo/pulse/extension/it/query/todo/TodoJdbcProjectionRepository.java` as the PostgreSQL example. It stores the complete `TodoProjection` directly in one JSONB document, with `todo_id` as the separate technical key for lookup and upsert. Avoid an intermediate storage DTO or manually flattening each domain field into SQL columns.
- Inject the configured `ObjectMapper` into repositories so runtime serializers and sensitive-value handling apply to persistence. Create the fixture table through Flyway before test cleanup, not through repository `@PostConstruct` DDL or `@Startup`. The migration is `query/integration-tests/src/test/resources/db/migration/V1__create_todo_projection.sql`.
- The integration-test module depends on `quarkus-flyway` and `flyway-database-postgresql`, with `quarkus.flyway.migrate-at-start=true` in `application.properties`. Pulse enables `baseline-on-migrate` for the schema initialized by Dev Services; this fixture sets `quarkus.flyway.baseline-version=0` so migration `V1` runs rather than being skipped by the default baseline version of `1`.
- Keep reusable `ProjectionId` serialization in `query/runtime/.../serialization`, including its mixin and mapper customizer; register the customizer through deployment `JacksonProcessor`. Keep Todo-specific mixins in the integration-test fixture.
- `ProjectionIdSerializer` writes `id` as a string and `linkTo` as an object. Its contextual `StdDeserializer` uses the concrete generic aggregate ID type to deserialize `linkTo`, preserves distinct projection IDs, and validates the ID prefix. Use typed properties or `TypeReference<ProjectionId<ConcreteId>>` at the root; a raw `ProjectionId` is insufficient. Do not replace this with a list of domain-specific `@JsonSubTypes` or a global `Identifiable` mixin.
- `TodoObjectMapperCustomizer.internal(ObjectMapper)` configures the fixture's mixins for plain JUnit tests. `DescriptionMixin` applies `@Sensitive` without adding infrastructure annotations to the domain record.

### Endpoint DTOs and query discovery

- API fixtures live under `com.damdamdeo.pulse.extension.it.api.todo`; persistence and fixture Jackson configuration live under `com.damdamdeo.pulse.extension.it.query.todo`.
- Construct `TodoDTO` from the domain `Todo` interface, allowing the same mapping for a projection or aggregate. Retain value-object types in the DTO when requested.
- Persistence encryption and an authorized API response can have different JSON representations. `TodoDTO.description` remains a `Description` in Java, but property-level `@JsonSerialize(converter = ...)` and `@JsonDeserialize(converter = ...)` using `StdConverter` map it to a plain JSON string. This leaves the projection's `@Sensitive` persistence mapping intact. A JAX-RS `ParamConverter` handles URL parameters, not DTO JSON fields.
- Let the deployment processor discover and register query use cases; do not add a redundant CDI producer. When using fixtures from the `pulse-core` test-jar, index that dependency in `src/test/resources/application.properties`:

```properties
quarkus.index-dependency.pulse-core-tests.group-id=com.damdamdeo.pulse
quarkus.index-dependency.pulse-core-tests.artifact-id=pulse-core
quarkus.index-dependency.pulse-core-tests.classifier=tests
```

- Discovery also activates generated query guards. Respect the use case's permissions in tests. `ListDoneTodosQueryUseCase` permits specific jobs; its endpoint test supplies an authorized job through a test-only alternative `ExecutionContextProvider` rather than bypassing guards or changing domain permissions.

### Choosing and writing tests

- Use plain JUnit 5 for converter, serializer, and DTO behavior; add Mockito only for collaborators the test needs. Initialize the mapper outside the test method. A plain DTO mapping test does not need `EncryptionModuleCustomizer` or encryption mocks. Tests proving `@Sensitive` encryption/decryption should configure the real Jackson module with mocked cryptographic services.
- Use `@QuarkusTest` and inject `ObjectMapper` to validate the assembled runtime and deployment registration. Do not manually instantiate mapper customizers or cryptographic services in that test. `TodoObjectMapperCustomizerQuarkusTest` checks a complete projection round trip and the absence of sensitive plaintext in JSON.
- Put Quarkus test configuration in `src/test/resources/application.properties` instead of a dedicated serialization profile. Let PostgreSQL Dev Services supply the JDBC URL; do not hardcode a localhost URL or disable Dev Services to work around a test-launch environment issue.
- For HTTP tests, seed the real projection repository, then extract `List<TodoDTO>` with RestAssured `TypeRef<List<TodoDTO>>` and compare it using a single AssertJ `containsExactly(...)`. Purge `todo_projection` through a `teardown()` method annotated with both `@BeforeEach` and `@AfterEach`, rather than a `try/finally` inside each test.
- Leave a blank line between test properties, use Java text blocks for JSON fixtures, and group multiple independent JUnit assertions with `assertAll(...)`. Preserve the single `containsExactly(...)` assertion for endpoint list responses.
- A JUnit discovery error such as “Could not load class” may wrap a Quarkus augmentation failure: inspect the deepest cause before changing JUnit configuration. Check projection validation, dependency indexing, and CDI wiring.
- Dev Services requires access to Docker. A socket error such as `operation not permitted` from an agent sandbox does not establish that Docker is stopped. Use the permitted execution mechanism for Docker access and report the actual blocker; do not alter datasource configuration to hide it. The integration-test Maven configuration also supplies the Java 24+ `--add-opens=java.base/java.lang=ALL-UNNAMED` option needed by the runtime used here.

## Testing and validation

- Use `query/deployment/src/test/java` as the main reference for Quarkus-level behavior. File query, repository, endpoint, and generated guard tests are grouped by feature.
- Use the existing test resources under `query/deployment/src/test/resources` for PostgreSQL, OpenAPI, and file-processing cases. Keep tests focused on observable responses, stored data, privacy behavior, and augmentation errors.
- Run the narrowest affected tests first; expand to relevant module suites when changes or unresolved failures justify it:

```bash
mvn -pl query/runtime test
mvn -pl query/deployment test
```

- To verify integration behavior and runtime serialization with reactor dependencies, select the affected tests explicitly, for example:

```bash
mvn -pl query/integration-tests -am \
  -Dtest=TodoEndpointTest,TodoObjectMapperCustomizerQuarkusTest,ProjectionIdSerializationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- If a change crosses runtime and deployment, verify both. If it changes a core query contract, also run the relevant `core` tests.

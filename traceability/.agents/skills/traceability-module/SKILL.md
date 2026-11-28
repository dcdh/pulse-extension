---
name: traceability-module
description: Use or change the Pulse Traceability extension, including its REST finder routes, paginated execution details, per-actor counters, configurable read role, PostgreSQL repositories, and Quarkus contract tests.
---

# Pulse Traceability

## REST finder

| GET route | Response DTO | Top-level JSON fields |
| --- | --- | --- |
| `/traceability/finder/byIdentifiable/{identifiable}` | `AggregateIdInvolvedDTO` | `identifiable`, `linkToAggregateRootId`, `page`, `nbOfExecutions` |
| `/traceability/finder/byExecutedByHashed/{executedByHashed}` | `ExecutedByInvolvedDTO` | `page` |

- Pass `page[index]` (zero-based) and `page[size]` on both routes.
- Use the exact identifier for `byIdentifiable`. The aggregate-root link can differ from the requested identifier.
- Use the complete typed hash for `byExecutedByHashed`, such as `EU:alice-hashed`. Read the actor fields from each execution detail.
- Read the entries and pagination flags from `page.listOfInvolved`, `page.totalPages`, `page.hasNext`, and `page.hasPrevious`.
- Each detail contains numeric `traceId` and `correlationId`, `executedByHashed`, `executedBy`, `source`, `executionStatus`, `from`, `executedAt`, and `nbOfExecutions`. Event metadata `eventType` and `aggregateVersion` is optional.
- `nbOfExecutions` is a list of actor counters on the identifiable response and a single actor's counters on each detail. Each counter object contains `executedBy` and the integer fields `commandNbOfTimes`, `commandUnauthorizedNbOfTimes`, `commandBusinessFailedNbOfTimes`, `queryNbOfTimes`, `queryUnauthorizedNbOfTimes`, and `traceabilityNbOfTimes`. Counters describe the actor's involvement for an identifier. Multiple detail rows, such as events produced by one command, can carry the same counters. Compare aggregate counter entries by actor when order is irrelevant.

## Configuration and access

- Set `pulse.traceability.tracing-mode` to `INVOLVED` or `INVOLVED_WITH_FULL_DETAILS` to register the REST finder. The default is `DISABLED`. Use `INVOLVED_WITH_FULL_DETAILS` when the application needs persisted execution details.
- `pulse.traceability.role` configures read access and defaults to `ROLE_TRACEABILITY_READ`. `SmallryeTraceabilityRoleProvider` reads the setting, and `TraceabilityFeature.isEnabled()` checks the current execution context for that role. Supply the configured role in finder test fixtures.
- `FinderExceptionMapper` converts a finder failure caused by `UnauthorizedException` to HTTP 403. The REST fixture uses `BOOM` as the identifier to exercise HTTP 500 with an `application/problem+json` response and the title `Internal Server Error`.

## REST contract tests

The test requests use the following routes and URL-encoded pagination names.

```http
GET /traceability/finder/byIdentifiable/U000001-T000001?page%5Bindex%5D=0&page%5Bsize%5D=10
GET /traceability/finder/byExecutedByHashed/EU:alice-hashed?page%5Bindex%5D=0&page%5Bsize%5D=10
```

- In RestAssured, use `.queryParam("page[index]", "0")` and `.queryParam("page[size]", "10")`. Assert nested response paths such as `page.listOfInvolved[0].executedByHashed` and `page.listOfInvolved[0].nbOfExecutions.commandNbOfTimes`.
- `StubInvolvedFinder` returns two entries for the identifiable request and one for the hash request. In both cases the page has one total page and both navigation flags are false.
- Use `GET /q/openapi?format=json` to check the route parameters and response schema references when changing DTOs, routes, or serialization.

From the repository root, read `TraceabilityInvolvedFinderEndpoint.java` under `traceability/runtime/src/main/java/com/damdamdeo/pulse/extension/traceability/runtime/api/`. It adapts the core `InvolvedFinder` and owns the REST DTOs. Check `TraceabilityObjectMapperProducer` under `api/serialization/` when changing JSON representations of domain values.

Read `TraceabilityInvolvedFinderEndpointTest.java` under `traceability/deployment/src/test/java/com/damdamdeo/pulse/extension/traceability/deployment/api/` for the REST assertions and OpenAPI contract.

```bash
mvn -pl traceability/deployment -am \
  -Dtest=TraceabilityInvolvedFinderEndpointTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

For PostgreSQL behavior, use the repository and finder integration tests in `traceability/deployment`. Generate fixture trace IDs through `TraceIdGenerator` when the same test also exercises the real finder, since finder calls append their own traces.

## OpenAPI schema

The following OpenAPI 3.1 excerpt contains both finder routes and all schemas they reference. It comes from the deployment test application. Use `GET /q/openapi?format=json` to inspect the complete document for a running application.

```yaml
openapi: 3.1.0
info: {title: TodoTaking API, version: 1.0.0-SNAPSHOT}
paths:
  /traceability/finder/byExecutedByHashed/{executedByHashed}:
    get:
      parameters:
      - name: executedByHashed
        in: path
        required: true
        schema: {$ref: '#/components/schemas/ExecutedByHashed'}
      - required: true
        schema: {type: integer}
        name: page[index]
        in: query
      - required: true
        schema: {type: integer}
        name: page[size]
        in: query
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: {$ref: '#/components/schemas/ExecutedByInvolvedDTO'}
      summary: Find By Executed By Hashed
      tags:
      - Traceability Involved Finder Endpoint
  /traceability/finder/byIdentifiable/{identifiable}:
    get:
      parameters:
      - name: identifiable
        in: path
        required: true
        schema: {$ref: '#/components/schemas/AnyIdentifiable'}
      - required: true
        schema: {type: integer}
        name: page[index]
        in: query
      - required: true
        schema: {type: integer}
        name: page[size]
        in: query
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: {$ref: '#/components/schemas/AggregateIdInvolvedDTO'}
      summary: Find By Identifiable
      tags:
      - Traceability Involved Finder Endpoint
components:
  schemas:
    AggregateIdInvolvedDTO:
      type: object
      required:
      - identifiable
      - linkToAggregateRootId
      - page
      - nbOfExecutions
      description: Execution details and counts for an identifiable.
      properties:
        identifiable: {type: string, description: Identifier of the identifiable.}
        linkToAggregateRootId: {type: string, description: Link to the aggregate root identifier.}
        page: {$ref: '#/components/schemas/DetailedInvolvedPageDTO', type: object, description: Page of execution details.}
        nbOfExecutions:
          type: array
          items: {$ref: '#/components/schemas/NbOfExecutionsDTO'}
          description: Execution counts by actor.
    AnyIdentifiable:
      type: object
      properties:
        id: {type: string}
    DetailedInvolvedDTO:
      type: object
      required:
      - traceId
      - correlationId
      - executedByHashed
      - executedBy
      - source
      - executionStatus
      - from
      - executedAt
      - nbOfExecutions
      description: Actor involved in the execution of an identifiable.
      properties:
        traceId: {type: [integer, number], format: int64, description: Trace id.}
        correlationId: {type: [integer, number], format: int64, description: Correlation id.}
        executedByHashed: {type: string, description: Hashed identifier of the actor who executed the operation.}
        executedBy: {type: string, description: Information identifying the actor who executed the operation.}
        eventType: {type: string, description: Event type.}
        aggregateVersion: {type: [integer, number], format: int32, description: Aggregate version.}
        source: {type: string, description: Source}
        executionStatus: {type: string, description: ExecutionStatus}
        from: {type: string, description: From}
        executedAt: {type: string, description: executedAt}
        nbOfExecutions: {$ref: '#/components/schemas/NbOfExecutionsDTO', type: object, description: Execution counts
            for this actor.}
    DetailedInvolvedPageDTO:
      type: object
      required:
      - listOfInvolved
      - totalPages
      - hasNext
      - hasPrevious
      description: Page of execution details.
      properties:
        listOfInvolved:
          type: array
          items: {$ref: '#/components/schemas/DetailedInvolvedDTO'}
          description: List of execution details.
        totalPages: {type: integer, format: int32, description: Total number of pages.}
        hasNext: {type: boolean, description: Whether another page is available after the current page.}
        hasPrevious: {type: boolean, description: Whether a page is available before the current page.}
    ExecutedByHashed:
      type: object
      properties:
        hashed: {type: string}
    ExecutedByInvolvedDTO:
      type: object
      required:
      - page
      description: Execution details for an actor.
      properties:
        page: {$ref: '#/components/schemas/DetailedInvolvedPageDTO', type: object, description: Page of execution details.}
    NbOfExecutionsDTO:
      type: object
      required:
      - executedBy
      - commandNbOfTimes
      - commandUnauthorizedNbOfTimes
      - commandBusinessFailedNbOfTimes
      - queryNbOfTimes
      - queryUnauthorizedNbOfTimes
      - traceabilityNbOfTimes
      description: Number of executions.
      properties:
        executedBy: {type: string, description: Information identifying the actor who executed the operation.}
        commandNbOfTimes: {type: integer, format: int32, description: Command number of times.}
        commandUnauthorizedNbOfTimes: {type: integer, format: int32, description: Command unauthorized number of times.}
        commandBusinessFailedNbOfTimes: {type: integer, format: int32, description: Command business failed number of
            times.}
        queryNbOfTimes: {type: integer, format: int32, description: Query number of times.}
        queryUnauthorizedNbOfTimes: {type: integer, format: int32, description: Query unauthorized number of times.}
        traceabilityNbOfTimes: {type: integer, format: int32, description: Traceability number of times.}
```

---
name: traceability-module
description: Use or change the Pulse Traceability finder endpoints, including actor involvement counts, detailed execution traces, identifier and actor-hash filters, pagination, and Quarkus REST contract tests.
---

# Pulse Traceability

## Choose the finder

| GET route | Result |
| --- | --- |
| `/traceability/finder/involved/byIdentifiable/{identifiable}` | Actor involvement counts for the identifiable |
| `/traceability/finder/involved/byExecutedByHashed/{executedByHashed}` | Actor involvement counts across identifiables matching the actor hash |
| `/traceability/finder/detailed/byIdentifiable/{identifiable}` | Individual execution records for the identifiable |
| `/traceability/finder/detailed/byExecutedByHashed/{executedByHashed}` | Individual execution records matching the actor hash |

- Pass `page[index]` (zero-based) and `page[size]` on every route. The response contains `listOfInvolved`, `totalPages`, `hasNext`, and `hasPrevious`.
- Both `byIdentifiable` routes accept `includeUncompounded`, which defaults to `false`. Set it to `true` for prefix matching so that compound identifiers starting with the supplied identifier can also be included. Both JDBC finders apply this option to the result selection and the pagination count.
- Both `byExecutedByHashed` routes take the full typed hash, such as `EU:alice-hashed`, as a path parameter.
- Both response types carry `identifiable`, `linkToAggregateRootId`, `executedByHashed`, and `executedBy`. Keep the identifiable and the aggregate root link distinct when consuming these fields.
- The involvement DTO adds `commandNbOfTimes`, `commandUnauthorizedNbOfTimes`, `commandBusinessFailedNbOfTimes`, `queryNbOfTimes`, `queryUnauthorizedNbOfTimes`, and `traceabilityNbOfTimes`.
- The detailed DTO adds numeric `traceId` and `correlationId`, plus `source`, `executionStatus`, `from`, and `executedAt`. Its OpenAPI schema also exposes optional `eventType` and `aggregateVersion`.

## Examples from the deployment tests

The following requests use the encoded forms of the pagination parameter names.

```http
GET /traceability/finder/involved/byIdentifiable/U000001-T000001?includeUncompounded=true&page%5Bindex%5D=0&page%5Bsize%5D=10
GET /traceability/finder/detailed/byIdentifiable/U000001-T000001?includeUncompounded=true&page%5Bindex%5D=0&page%5Bsize%5D=10
GET /traceability/finder/involved/byExecutedByHashed/EU:alice-hashed?page%5Bindex%5D=0&page%5Bsize%5D=10
GET /traceability/finder/detailed/byExecutedByHashed/EU:alice-hashed?page%5Bindex%5D=0&page%5Bsize%5D=10
```

- The identifiable requests return HTTP 200 with Alice and Bob in the test fixture. The involvement response gives each actor a `commandNbOfTimes` of `1`. The detailed response gives each record `traceId: 1`, `source: "COMMAND"`, `executionStatus: "SUCCESS"`, `from: "from"`, and `executedAt: "2026-09-06T12:00:00Z"`.
- The actor-hash requests return HTTP 200 with one entry for `EU:alice-hashed`, with `executedBy` equal to `EU:alice@mail.com`. All four examples return `totalPages: 1`, `hasNext: false`, and `hasPrevious: false`.
- The exception tests use `BOOM` as the fixture identifier to trigger a finder failure. Both routes return a problem response with HTTP 500 and the title `Internal Server Error`, with an empty stack trace.
- In RestAssured, pass the raw parameter names with `.queryParam("page[index]", "0")` and `.queryParam("page[size]", "10")`. The tests also verify the published contract with `GET /q/openapi?format=json`.

## Repository locations

From the Pulse extension repository root, find `TraceabilityFinderInvolvedEndpoint.java` and `TraceabilityFinderDetailedInvolvedEndpoint.java` in `traceability/runtime/src/main/java/com/damdamdeo/pulse/extension/traceability/runtime/api/`. These JAX-RS adapters call `InvolvedFinder` and `DetailedInvolvedFinder` and map their pages to the endpoint DTOs.

Read `TraceabilityFinderInvolvedEndpointTest.java` and `TraceabilityFinderDetailedInvolvedEndpointTest.java` in `traceability/deployment/src/test/java/com/damdamdeo/pulse/extension/traceability/deployment/api/` when changing the HTTP contract. Run both after changes to routes, DTOs, or serialization.

```bash
mvn -pl traceability/deployment -am \
  -Dtest=TraceabilityFinderInvolvedEndpointTest,TraceabilityFinderDetailedInvolvedEndpointTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

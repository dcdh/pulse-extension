= TODO

== Must have
2. Quarkus batchlet with reader as a projection reader and writer as a domain use case - Add a simple test - create a specific extension
   https://docs.quarkiverse.io/quarkus-jberet/dev/chunk-processing.html
3. rewrite the integration test to reflect the reality, same package than facturation, remove creational part, need two command, creational and mark as done, do the query, use DTO whit domain object and openapi, dot the tests
batch must be locked and schedulable
2. traceability: front : avatar
5. faire les skills
7. OwnedByProvider replace by ExecutedByResolver
== Nice to have
1. propage traceId in CommandHandler on onStoredEventListener#execute
2. split common module into separate sub modules
3. tests sharing
4. Event kafka: do not use `<JsonNode>` find a way to replace it with the record. Other parameters must be transmitted in headers.
   The responsibility to deserialize the event must be done before calling the handlers.

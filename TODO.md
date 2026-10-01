= TODO

== Must have
1. job, reader et usecase call, job with jobname, locking mechanism
2. traceability: do the endpoint, use a cursor pagination, use a way to customize message depending on the event, likes on aggregate, link command with events produced, say if full details or not depending of configuration
2. traceability: front : avatar
5. faire les skills
7. OwnedByProvider replace by ExecutedByResolver
== Nice to have
1. propage traceId in CommandHandler on onStoredEventListener#execute
2. split common module into separate sub modules
3. tests sharing
4. Event kafka: do not use `<JsonNode>` find a way to replace it with the record. Other parameters must be transmitted in headers.
   The responsibility to deserialize the event must be done before calling the handlers.

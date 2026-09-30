= TODO

== Must have
1. traceability: do the endpoint, use a cursor pagination, use a way to customize message depending on the event, likes on aggregate, link command with events produced
2. traceability: front : avatar
3. feature flag basic: expose an interface and multiples implementation for traceability, audit ...
4. job implementation associated to a use case with locking mechanism, locking should exit when processing
6. OwnedByProvider replace by ExecutedByResolver
== Nice to have
1. propage traceId in CommandHandler on onStoredEventListener#execute
2. split common module into separate sub modules
3. tests sharing
4. Event kafka: do not use `<JsonNode>` find a way to replace it with the record. Other parameters must be transmitted in headers.
   The responsibility to deserialize the event must be done before calling the handlers.

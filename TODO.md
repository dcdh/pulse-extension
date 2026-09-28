= TODO

== Must have
1. audit: do the endpoint, use a cursor, use a way to customize message depending on the event, likes on aggregate
2. sequence generator: synchronized, select for update ?
3. Audit: front : avatar
4. do a review command should be read commited, query not read commited
5. feature flag basic: expose an interface and multiples implementation for traceability, audit ...
6. OwnedByProvider replace by ExecutedByResolver
== Nice to have
1. propage traceId in CommandHandler on onStoredEventListener#execute
2. split common module into separate sub modules
3. tests sharing
4. Event kafka: do not use `<JsonNode>` find a way to replace it with the record. Other parameters must be transmitted in headers.
   The responsibility to deserialize the event must be done before calling the handlers.

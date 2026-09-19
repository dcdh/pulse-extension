= TODO

== Must have
1. OwnedByProvider replace by ExecutedByResolver
2. Event kafka: do not use `<JsonNode>` find a way to replace it with the record. Other parameters must be transmitted in headers.
The responsibility to deserialize the event must be done before calling the handlers.
== Nice to have
1. propage traceId in CommandHandler on onStoredEventListener#execute
2. split common module into separate sub modules
3. tests sharing

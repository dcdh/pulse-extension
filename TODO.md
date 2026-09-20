= TODO

== Must have
1. permission: write add a domainId one with command
2. permission: query add a domainId one
3. traceability store if Query (Q) or Command (C) faire un count pour la partie involved et rajouter C ou K dans la partie detail
4. traceability store if ok ko (count ok, count ko)
5. OwnedByProvider replace by ExecutedByResolver
== Nice to have
1. propage traceId in CommandHandler on onStoredEventListener#execute
2. split common module into separate sub modules
3. tests sharing
4. Event kafka: do not use `<JsonNode>` find a way to replace it with the record. Other parameters must be transmitted in headers.
   The responsibility to deserialize the event must be done before calling the handlers.

= TODO

== Must have
1. In GuardQuery audience add a ROLE_RESTRICTED with a name, and an audience for a specific Service Account and an Audience for a specific End User
2. Create a GuardDomainUseCase using the same audience than GuardQuery
- I would need to have a Creational one

== Nice to have
1. propage traceId in CommandHandler on onStoredEventListener#execute
2. split common module into separate sub modules
3. tests sharing

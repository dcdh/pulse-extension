= TODO

== Must have
1. Query is a UseCase, rename it to QueryUseCase. I've got ambiguity with the UseCase interface. Should handle it !
2. In GuardQuery audience add a ROLE_RESTRICTED with a name, and an audience for a specific Service Account and an Audience for a specific End User
3. Create a GuardUseCase using the same audience than GuardQuery

== Nice to have
1. propage traceId in CommandHandler on onStoredEventListener#execute
2. split common module into separate sub modules
3. tests sharing

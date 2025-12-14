# Consumer

TODO documentation regarding event publication

## Notification

- Notify last pushed messages
- Each application instance must consume the last messages
- Retention time : the shortest
- Expose notification using SSE
- Filter notification depending on a context
- Multiple SSE endpoint must be available to contextualise (or maybe one with a Generic parameter in Json for exemple)
- Consumer must be in latest message consumption
- Possibility to enrich the message - use custom DTO to push

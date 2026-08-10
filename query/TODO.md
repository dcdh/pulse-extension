# TODO

1. expose endpoints
   - DownloadQuery
   - GetFileInfoQuery
   - GetTraceByFileIdentifierQuery
   - e2e: upload, download, getFileInfo, getTraceByFileIdentifier
2. add query log on GuardQuery
   - share code with file traceability
   - on each Projection Result retrieve the list of aggregateIds
   - store executedBy, executedAt and an UUID identifier
   - only append
   - disallow update, delete

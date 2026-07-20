= TODO

== Nice to have

0. quarkus-jdbc-postgresql : do not include inside dependency, check presence using dependency check, remove Provider<DataSource>, include beans only if dependency is present, do a documentation about it.
1. pulse-cache
- caffeine
- redis (valkey)
- test-framework: create a Stub
- cache should not fail: use a degraded mode by doing the call and add a log.
2. split common module into separate sub modules
3. tests sharing

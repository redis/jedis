# Jedis Coding Conventions
Jedis is synchronous Java client for Redis, shipped as a library on Maven Central as `redis.clients:jedis`.
It covers standalone, cluster, sentinel, and multi-db setups — with pipelining, transactions, pub/sub, and Redis Stack modules — and targets Java 8 for source/binary compatibility.

## General Principles
- **Maintain JDK 8 compatibility.** All production and test code must compile and run on JDK 8.
- **Keep documentation up to date.** If a change affects user-facing behavior, configuration, or the public API, update the relevant pages under [`docs/`](docs) (MkDocs).
- **Add or update tests.** Every bug fix should include a regression test, and every new feature should include appropriate tests. Follow the conventions in [`docs/integration-testing.md`](docs/integration-testing.md) for choosing between unit and integration tests.
- **Document breaking changes.** Any breaking change must be documented in the appropriate migration guide under [`docs/migration-guides/`](docs/migration-guides).
- **Follow the contribution guidelines.** Ensure all changes comply with the project rules in [`.github/CONTRIBUTING.md`](.github/CONTRIBUTING.md).
- **Request approval before adding dependencies.** Do not introduce new runtime, test, or build dependencies without explicit confirmation.
- **Review-friendly changes.** Keep commits and pull requests focused and logically grouped. Separate feature or bug-fix changes from refactoring, formatting, and other mechanical changes whenever practical.

## Development Environment
- **Use the CI JDK version.** Build and test using the same JDK version configured in the GitHub Actions workflows. Check `.github/workflows/` (or `docs/integration-testing.md`) to determine the required version before reproducing CI failures.

## Build & Test Commands
- `make start version=8.6` + `mvn clean verify` + `make stop` — full run against
  the Docker Redis env (`make test` does all three).
- `mvn test` — unit tests (no Redis); `mvn -Dtest=ClassName test` for one class.

## Implementation Guidelines
### Code Style
- Keep comments short and informative. Comment **why**, not **what**. Avoid comments that simply restate the code.
- Remove unused imports. Do not use wildcard imports. Avoid fully qualified class names unless necessary to resolve naming conflicts.
- **Annotate all new public API with `@since`.** Determine the version from the current build by running:
  ```sh
  mvn help:evaluate -Dexpression=project.version -q -DforceStdout
  ```
  (or by reading the top-level `<version>` in `pom.xml`), then remove the `-SNAPSHOT` suffix. For example, `8.0.0-SNAPSHOT` becomes `@since 8.0`.

### Test Conventions
- Name new unit test classes using the `*Test` convention.
- Name new integration test classes using the `*IT` convention. Do not use the `integration` tag for new tests.

## Architecture

See [`docs/redis-client-components-overview.md`](docs/redis-client-components-overview.md)
for a high-level walkthrough (executors, providers, builders, and command execution flows).

`UnifiedJedis` is the core client. It implements the command interfaces and
delegates to three collaborators:

- `ConnectionProvider` (`providers/`) — obtains connections: pooled, cluster,
  sentinel, or multi-db.
- `CommandExecutor` (`executors/`) — runs a command: simple, retry, cluster
  routing, or failover.
- `CommandObjects` — factory building a typed `CommandObject<T>` per command.

**Modern clients** extend `UnifiedJedis` and are built through
`AbstractClientBuilder` subclasses (`builders/`): `RedisClient`,
`RedisClusterClient`, `RedisSentinelClient`, `MultiDbClient`. A new client
overrides `createDefaultConnectionProvider()`, `createDefaultCommandExecutor()`,
`createClient()`, and `validateSpecificConfiguration()` (run before `build()`).

**Legacy client:** `Jedis` is the single-connection client (pooling via
`JedisPool`), still supported. `JedisPool`, `JedisCluster`, and
`JedisSentinelPool` are `@Deprecated` in favor of the builder-based clients.

**Feature modules** expose dedicated command interfaces under
`redis.clients.jedis.<module>`: `search`, `json`, `bloom`, `timeseries`, plus
`csc` (client-side caching) and `mcf` (multi-db / failover).

## Conventions

**Adding or changing a command** — trace an existing command first, then update
the full matrix so all surfaces stay in sync:

1. **Command interfaces** (`commands/`): `<Group>Commands` and
   `<Group>BinaryCommands` (String vs `byte[]`), each with its `<Group>Pipeline…`
   variant.
2. **`CommandObjects`** — build the args and a response `Builder<T>`, e.g.
   `new CommandObject<>(commandArguments(GET).key(key), BuilderFactory.STRING)`.
   `ClusterCommandObjects` overrides these to enforce cluster constraints.
3. **`UnifiedJedis`** and **`PipeliningBase`** — the execution entry points.
4. **`Jedis`** — add it here too, for backward compatibility.

**Encoding:**
- String ↔ bytes: `SafeEncoder.encode()` — never `String.getBytes()` (breaks GBK).
- Numeric → bytes: `Protocol.toByteArray()`.

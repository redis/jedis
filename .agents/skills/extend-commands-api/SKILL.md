---
name: extend-commands-api
description: Add or extend Redis commands in the Jedis client API — a new core command, a family of new commands, an extension to an existing command's options, or a module command (Search/TimeSeries/JSON/Bloom). Gathers evidence (Redis server PR, HLD document), plans the full implementation matrix in plan mode, then implements with unit and integration tests following Jedis maintainer conventions.
---

# Extend the Jedis Commands API

Implement a new Redis command (or extend an existing one) in Jedis, following the
conventions Jedis maintainers enforce in review.

## Phase 0 — Gather evidence BEFORE planning

Do all of the following before writing any plan or code:

1. **Ask the user for the HLD.** Interactively ask the user for the path to a
   markdown file containing the High-Level Design for the command(s) (or confirm
   that no HLD exists). If a path is given, read it fully — it is the primary
   source for syntax, semantics, reply shape, and edge cases.
2. **Find the server-side PR in the `redis/redis` GitHub repo.** First verify
   that `gh` works in the current (sandboxed) environment:
   ```bash
   gh auth status
   ```
   Sandboxes often block access to credential files (`~/.netrc`, gh config), so
   `gh` may report unauthenticated here even though it works on the user's
   machine. If `gh auth status` fails or reports no authentication, **ask the
   user for permission to run the `gh` commands outside the sandbox** (e.g. with
   the sandbox override for these specific read-only commands), explaining that
   `gh` cannot reach its credentials from inside the sandbox. Only if the user
   declines (or `gh` is genuinely not logged in anywhere) fall back to the
   unauthenticated GitHub REST API or fetching
   `https://github.com/redis/redis/pulls?q=<command>`.

   Then search for the PR that adds/extends the command on the server:
   ```bash
   gh search prs --repo redis/redis "<COMMAND NAME>" --limit 10
   gh pr view <num> --repo redis/redis
   gh pr diff <num> --repo redis/redis   # look at src/commands/*.json for exact syntax
   ```
   From the server PR,
   extract: exact wire syntax (argument order and optionality), reply type per
   RESP2/RESP3, error conditions, time complexity, and — critically — the **first
   server version carrying the feature** (RC builds like `8.7.225` = Redis 8.8 RC
   are used for test gating). Note whether the server marks the feature
   *experimental / in preview*.
3. **Verify the command exists on the "next" Redis OSS version.** Start the
   integration environment using the **latest** `.env.vX.XX` file under
   `src/test/resources/env/` (pick the highest version numerically, not
   lexicographically — e.g. `8.10` > `8.8`):
   ```bash
   ls src/test/resources/env/.env.v* | sort -V | tail -1   # → e.g. .env.v8.10
   make start version=8.10
   ```
   Then probe the `standalone0` endpoint. Read its connection details from
   `src/test/resources/endpoints.json` (currently `redis://localhost:6379`,
   password `foobared`) and check the target command via redis-cli:
   ```bash
   redis-cli -u redis://localhost:6379 -a foobared INFO server | grep redis_version
   redis-cli -u redis://localhost:6379 -a foobared COMMAND INFO <COMMAND>   # non-empty → command exists
   redis-cli -u redis://localhost:6379 -a foobared COMMAND DOCS <COMMAND>   # arity/args — compare with the server PR
   ```
   For a NEW command, `COMMAND INFO` must return a non-empty reply. For an
   EXTENDED command, additionally invoke it with the new syntax against a
   scratch key and confirm the server accepts it (no `ERR syntax error` /
   `ERR unknown argument`).

   **If the command/option is missing on the latest env version**, the image tag
   pinned in the `.env.vX.XX` file is too old for the target change. Interactively
   **ask the user for a `redislabs/client-libs-test` image tag** that contains it
   (e.g. an RC/edge/milestone build — tags are listed at
   https://hub.docker.com/r/redislabs/client-libs-test/tags; you may check them
   yourself and suggest candidates). Then restart the environment with that tag
   and re-run the probes:
   ```bash
   make stop
   make start CLIENT_LIBS_TEST_IMAGE_TAG=<tag>
   ```
   If the user has no tag to offer (or the probe still fails), report it and
   proceed anyway: the implementation can continue, but integration tests will
   be gated/skipped until a `redislabs/client-libs-test` image ships the change.
   Keep the environment running for later test runs, or `make stop` if you
   won't need it soon.
4. **Create redis-cli showcase test cases.** Once the command is available on
   the running environment, derive a small set of redis-cli scenarios from the
   HLD and the server PR and run them against `standalone0`. These serve two
   purposes at once:
   - **Smoke test**: prove the documented behavior actually holds on the target
     server — happy path, each new option/keyword, reply shape (run with
     `redis-cli -3` too if RESP3 replies differ), edge cases and error
     conditions called out in the HLD (empty/missing key, out-of-range args,
     conflicting options), so the Java implementation is built against observed
     replies, not assumptions.
   - **Showcase**: each scenario should read as a mini use-case that explains
     WHY the command/option exists — what a user could not do (or could only do
     awkwardly) before, and how the new API covers it. Prefer realistic data
     over `foo`/`bar` (e.g. rate-limit counters for a bounded INCR, sensor
     readings for a time-series aggregator).

   Save the scenarios as a commented script in a scratch file (one block per
   use-case: a one-line "what this demonstrates" comment, the redis-cli
   commands, and the observed reply pasted back as comments). Carry this
   material forward: it feeds the Phase 1 plan (as the explanation of the
   feature and the source of expected values for tests), the Java test
   assertions, and the PR description. If the command could not be made
   available on any image (see step 3), still write the scenarios from the
   HLD/PR as *expected* transcripts and mark them unverified.
5. **Read `docs/integration-testing.md`** in the Jedis repo — it defines the test
   environment, endpoint discovery, `*Test` vs `*IT` naming, and how to run tests.
6. **Trace one analogous existing command** end-to-end in the codebase (same
   command group, similar reply shape) so the plan mirrors real code, not
   guesswork.
7. Determine the `@since` version:
   ```bash
   mvn help:evaluate -Dexpression=project.version -q -DforceStdout
   ```
   Strip `-SNAPSHOT` (e.g. `8.0.0-SNAPSHOT` → `@since 8.0`).

## Phase 1 — Plan mode, then explicit approval

Enter **plan mode**. Using the evidence, classify the change with the decision
tree below, enumerate the exact file-by-file touch list, the test matrix, and the
gating annotations. Open the plan with a short "what this feature enables"
section built from the redis-cli showcase scenarios (Phase 0 step 4), including
one or two representative command/reply transcripts, so the user sees the
use-case before the file list. Present the plan to the user and **explicitly ask
permission to execute in auto-accept mode** before implementing. Do not start
editing files until the user approves.

## Decision tree — what kind of change is this?

**A. Extension of an existing command that fits an existing params class**
(e.g. new enum value / new option token):
- Touch ONLY the params/model classes + tests. Do NOT touch command interfaces,
  CommandObjects, UnifiedJedis, PipeliningBase, or Jedis — the existing
  `IParams.addParams(CommandArguments)` delegation carries the new option through
  automatically, for both String and binary surfaces.
- If the option is a bare token: add an enum constant implementing `Rawable`
  (bytes come from `SafeEncoder.encode(name())` — self-wiring).
- If it's a new field: add a fluent setter returning `this`, extend
  `addParams()`, and **update `equals`/`hashCode`** in sync.
- If the reply shape grows (e.g. more array elements): extend the response model
  class backward-compatibly — keep old constructors/getters working, add new
  accessors, update the builder in the relevant `*BuilderFactory` to branch on
  reply size. Jedis does not defensively copy response collections (stated
  maintainer convention).

**B. New core command(s)**  — the FULL matrix, every layer:
1. `Protocol.java` — new `Command` enum constant(s); new `Keyword` constants for
   sub-tokens, grouped under a `// <FEATURE> keywords` comment, alphabetical.
   **Never add a Keyword that duplicates a token already carried by a dedicated
   `Rawable` enum** (dead keywords get flagged in review).
2. Command interfaces in `commands/`: `<Group>Commands`, `<Group>BinaryCommands`,
   `<Group>PipelineCommands`, `<Group>PipelineBinaryCommands`. For a whole new
   command *family*, create four new `<Family>*Commands` interfaces and add one
   `extends` entry to `JedisCommands`, `JedisBinaryCommands`, `PipelineCommands`,
   `PipelineBinaryCommands`.
3. `CommandObjects` — String and byte[] factory methods side by side under a
   `// <Feature> commands` section. `ClusterCommandObjects` needs overrides ONLY
   for multi-key commands requiring slot checks; single-key commands need nothing.
4. `UnifiedJedis` — one-line `@Override` delegating to
   `executeCommand(commandObjects.xxx(...))`.
5. `PipeliningBase` — `appendCommand(commandObjects.xxx(...))` returning
   `Response<T>`.
6. `Jedis` (legacy) — `checkIsInMultiOrPipeline(); connection.executeCommand(commandObjects.xxx(...))`.
7. `pom.xml` — add every new file to the **formatter-maven-plugin includes** list
   (this repo format-enforces an allowlist; new files must be registered).

**C. Extension needing new overloads / a new params class** (hybrid): new methods
go through the full matrix of B; the option plumbing follows the params
conventions below.

**D. Module command (search / timeseries / json / bloom)**:
- Module interfaces are **String-only** — there are NO `*BinaryCommands` variants
  for modules; never create them.
- Module commands/keywords live in the module protocol class
  (`TimeSeriesProtocol.TimeSeriesCommand/TimeSeriesKeyword`,
  `SearchProtocol.SearchCommand/SearchKeyword`, …), each an enum implementing
  `ProtocolCommand`/`Rawable` with `SafeEncoder.encode()`-cached bytes.
- Strongly prefer extending the module's params/builder classes over adding
  interface methods — PRs #4504 and #4534 shipped whole features with ZERO
  changes to interfaces, CommandObjects, UnifiedJedis, or PipeliningBase.
- Search aggregation builders (`Reducer` subclasses) build a `List<Object>` via
  `getOwnArgs()`; the parent computes `narg` automatically. Canonicalize clause
  order at serialization time regardless of builder call order.
- Module response parsing lives in the module's builder factory
  (`TimeSeriesBuilderFactory`, `SearchBuilderFactory`); search aggregation results
  are loosely typed and often absorb new reply content with no parsing changes.

## Binary (byte[]) variant policy

- **Core commands: full String/byte[] parity is mandatory** — all four
  interfaces, paired CommandObjects methods, and all three clients. Reviewers
  explicitly request binary integration tests too.
- Only **keys and textual values** get `byte[]` overloads. Numeric/structural
  args (`long index`, ranges, booleans, enums) stay identical.
- **Params classes are shared, never duplicated**: one params class serves both
  surfaces; if a param carries text, give the params class both `foo(String)` and
  `foo(byte[])` setters (see `ArgrepParams`).
- Return types mirror the key type (`String`→`byte[]`,
  `List<String>`→`List<byte[]>`); count/index/model results stay shared.
- **Modules: no binary variants at all.**

## Params class conventions (`params/`)

- Implement `IParams` with `addParams(CommandArguments args)`. Fluent setters
  return `this`; provide a static factory named after the class
  (`increxParams()`, `rangeParams()`).
- Do NOT extend another command's params hierarchy. If two overloads share
  options but differ in a typed field (long vs double), create a self-typed
  abstract base: `BaseFooParams<T extends BaseFooParams<T>>` with a private
  `self()` cast, plus two concrete subclasses — compile-time type safety over a
  polymorphic single class.
- Implement **`equals`/`hashCode`** (needed for Mockito matching in mocked tests)
  and keep them in sync with every new field. `toString` is not required on
  params classes.
- Validate eagerly in setters with descriptive `IllegalArgumentException` /
  `IllegalStateException` messages, consistent wording across sibling classes
  (e.g. "Aggregators must be non-null and non-empty"). Required-config checks may
  run at serialization time. No client-side server-version checks — an old server
  returning an error is acceptable and should be noted in the PR description.
- Expiration-style option groups are single-slot, last-wins (mirror `SetParams`).
- Document the wire-emission order in javadoc and lock it down in unit tests.

## Response mapping (`BuilderFactory` / `resps/`)

- **Reuse existing generic builders whenever the reply maps to a standard shape**
  (`LONG_LIST`, `DOUBLE_LIST`, `STRING`, `ENCODED_OBJECT_MAP`, …). Maintainers
  actively reject bespoke response classes for 2-element arrays and the like.
- Reuse `redis.clients.jedis.util.KeyValue<K,V>` for pair replies.
- When a model class is genuinely needed (map-shaped INFO replies), put it in
  `resps/`: public `String` constants for reply field names, a
  `Map<String,Object>`-taking constructor, typed getters, plus a raw-map accessor
  for forward compatibility; subclass for FULL/extended variants.
- Nullable numeric replies are boxed `Long` — **never `Optional`/`OptionalLong`**
  in the command API; consistency with existing signatures (e.g. `zrank`) wins.
- Multi-mode commands (one wire command, several reply types by mode) are split
  into distinctly-named typed Java methods (`aropAggregate`/`aropBitwise`/`aropCount`)
  rather than one polymorphic method. But mode selected by argument type uses
  **overloads of one name** (`increx(key, long, …)` / `increx(key, double, …)`),
  with the mode keyword (BYINT/BYFLOAT) implied by the overload.

## Encoding & enum rules

- String ↔ bytes: `SafeEncoder.encode()` — never `String.getBytes()`.
- Numeric → bytes: `Protocol.toByteArray()`.
- Token-valued argument enums live in `args/` and implement `Rawable` with
  `raw = SafeEncoder.encode(name())` cached in the constructor.
- Compose multi-token byte values (e.g. comma-joined lists) at the byte level.

## Javadoc, `@since`, `@Experimental`

- **Interface methods only** carry javadoc; implementations (`UnifiedJedis`,
  `Jedis`, `PipeliningBase`, `CommandObjects`) carry none.
- String-interface javadoc: `<b><a href="https://redis.io/commands/xxx">XXX
  Command</a></b>`, prose semantics, `Time complexity: O(...)`, `@param`,
  `@return`, `@since <version>`. Binary variants get short javadoc with
  `@see` to the String variant. Pipeline variants: one line — "Pipeline variant
  of {@link ...}" + `@since`.
- `@since` on every new public method and class, computed from `pom.xml`.
- If the server feature is **preview/experimental**, annotate ALL new public API
  (interfaces, methods, params, args, resps) with
  `redis.clients.jedis.annots.Experimental` and label the PR `experimental`.
  Do NOT use `@Experimental` for stable GA features.

## Test matrix — what to write

Follow `docs/integration-testing.md` for environment, layout, and naming. The
established per-command test layers (write all that apply):

1. **Params unit test** (`params/FooParamsTest`, plain `*Test`, no Redis):
   `@Nested` groups (`ValidationTests`, `AddParamsTests`, overload-equivalence),
   asserting exact wire args and order with
   `redis.clients.jedis.util.CommandArgumentsMatchers`
   (`hasArgumentCount`, `hasArguments`) and `RawableFactory.from(...)`; also test
   the `equals`/`hashCode` contract.
2. **Mocked delegation tests** (unit, Mockito): add `@Mock CommandObject<T>`
   fields to `MockedCommandObjectsTestBase` (name them by TYPE, e.g.
   `listLongCommandObject` — reuse existing ones when the type matches), then
   `when/verify` tests in `mocked/unified/UnifiedJedis<Group>CommandsTest` and
   `mocked/pipeline/PipeliningBase<Group>CommandsTest`.
3. **Unified integration tests**: add methods to the existing abstract
   `commands/unified/<Group>CommandsTestBase` when one exists; for a new family,
   create `<Family>CommandsTestBase extends UnifiedJedisCommandsTestBase` plus
   thin per-topology runners — standalone
   (`RedisClientCommandsTestHelper.getClient(protocol)`) and cluster
   (`ClusterCommandsTestHelper.getCleanCluster(protocol)`). **New concrete
   integration classes MUST be named `*IT`** — never `*IntegrationTest`, never
   `@Tag("integration")` on new classes.
4. **Cluster handling**: multi-key commands need cluster test overrides using
   hash-tagged keys (`s1{:}`) so keys share a slot; tests semantically
   incompatible with cluster get `@Test @Override` + `@Disabled("<reason>")`
   empty bodies.
5. **Legacy `Jedis` coverage**: extend `commands/jedis/<Group>*CommandsTest`
   (which extends `JedisCommandsTestBase`) — can be minimal (smoke/missing-key)
   when the unified base covers behavior fully. Binary variants get their own
   integration tests (reviewers ask for these explicitly).

Cross-cutting test conventions:
- **RESP2/RESP3** comes free: test bases are `@ParameterizedClass` +
  `@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")`
  (or `#jedisRespVersions` for legacy) — no per-test work; just make sure new
  classes inherit the right base.
- **Gating**: command already released → `@SinceRedisVersion("<RC build>")`
  (e.g. `"8.7.225"` for 8.8) at the shared base-class level ONCE — do not repeat
  it on subclasses (maintainers remove redundant ones). Command not yet in any
  GA server → `@EnabledOnCommand("<COMMAND>")` (capability probe via COMMAND
  INFO). Module presence as precondition → `assumeTrue(hasCommand(...))` probes.
  Environment exclusions → `@ConditionalOnEnv(value = TestEnvUtil.ENV_...,
  enabled = false)` (e.g. skip Redis Enterprise for brand-new features).
- Cover the **whole family the option touches** (store + non-store variants,
  with/without optional args, standalone + cluster), asserting real semantics,
  not just "no error". Avoid brittle assertions (no hard-coded `hashCode()`
  literals).

## Running the tests

Jedis pins the **CI JDK (Java 8)** for the test build — check
`.github/workflows/` for the exact version and use a matching local JDK. Verify
with `java -version` before running anything; a newer JDK may fail the build or
silently produce the wrong bytecode target.

- Unit tests (Surefire, no Redis): `mvn -B test`, or `mvn -Dtest=FooParamsTest test`.
- Integration tests (Failsafe) need the Docker env and the `verify` lifecycle:
  ```bash
  make start version=8.8        # pick the version that carries the new command
  mvn -B verify                 # or: mvn -Dtest=FooIT verify
  make stop
  ```
  Never run integration coverage via `mvn test`/`surefire:test` — Failsafe owns
  `*IT` classes. If a brand-new command isn't in any published
  `redislabs/client-libs-test` tag yet, say so: the integration tests will be
  skipped/gated (that is expected and acceptable — `@EnabledOnCommand` /
  `@SinceRedisVersion` handle it), but they must still be written and compile.

## PR hygiene checklist (verify before finishing)

- [ ] Every layer of the chosen matrix updated consistently (a return-type change
      must propagate across all 4 interfaces × 3 implementations × all test layers).
- [ ] New files added to the `pom.xml` formatter-plugin includes.
- [ ] `@since` (and `@Experimental` if preview) on all new public API.
- [ ] No unused imports, no wildcard imports, no dead `Protocol.Keyword` constants.
- [ ] `equals`/`hashCode` on params updated and unit-tested.
- [ ] `docs/` updated if user-facing behavior/configuration changed; migration
      guide entry if anything breaks.
- [ ] PR description states: server PR link, version gate choice and why, which
      layers intentionally did NOT change and why, and behavior against older
      servers.

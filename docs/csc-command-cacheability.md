# Client-Side Caching: Command Cacheability

This page documents how Jedis decides which commands are eligible for client-side caching
(CSC). The rules are by design aligned across Redis client libraries, so clients given the same
command metadata make the same cacheability decision.

## How it works

Jedis ships normalized Redis `COMMAND` metadata (flags, tips, key positions, and key specs),
generated from a known Redis release and also checked in as `CommandMetadata.json` in the
repository root. The default cacheability policy applies the eligibility rules below to that
metadata and resolves a verdict for every command Jedis can issue, container subcommands included
(keyed `PARENT|CHILD`, for example `MEMORY|USAGE`).

The policy is configured through `CacheConfig.Builder`:

- By default, the metadata-derived policy decides every command.
- `excludeCommands(...)` removes commands from the eligible set. Exclusions can only narrow: a
  command that is not cacheable by the default policy cannot be made cacheable. Exclusions apply
  to the exact command; a container subcommand is excluded individually by its `PARENT|CHILD`
  name (excluding a parent does not exclude its subcommands).
- `excludeCommands(Cacheable)` narrows per invocation: the given `Cacheable` is asked only about
  commands the default policy allows, and an invocation it reports as not cacheable is excluded,
  for example `excludeCommands((command, keys) -> !keys.contains("session:1"))`. It cannot make
  a denied command cacheable, and it combines with `excludeCommands(Set)` and with a per-client
  metadata resolver.
- Commands with no metadata have no verdict: they are not cacheable and are logged once per
  command name. Metadata for more commands is supplied at the metadata level, not as a caching
  decision — see [Per-client command metadata](#per-client-command-metadata) and
  `CommandMetadataResolver.withFallback(...)`. The eligibility rules always run on the resolved
  metadata.
- `cacheable(...)` (deprecated) replaces the policy with a user-provided `Cacheable`; it cannot
  be combined with exclusions. The custom policy alone decides, preserving pre-8.1 behavior, and
  a command it allows against the default policy is logged once as a warning. A client-level
  metadata resolver does not constrain it. It is deprecated in favor of
  `excludeCommands(...)`, which can narrow the cached set but never widen it, and is planned
  for removal in a future major release. Note that `CacheConfig.getCacheable()` returns the
  effective policy, which is an internal implementation — do not rely on identity with the
  instance you supplied or with `DefaultCacheable.INSTANCE`.

`DefaultCacheable` is deprecated; its verdicts now match the default policy.

## Eligibility rules

A command is client-side-cacheable only when **all** of the following hold:

1. It does **not** have the `dont_cache` command tip (an explicit negative override).
2. It has the `readonly` command flag. (The `@read` ACL category is *not* used: a read-only
   command can still be keyless, nondeterministic, blocking, or script-backed.)
3. It does **not** have the `blocking` command flag. Eligibility is resolved by command name, so
   `XREAD` is ineligible even when a particular invocation omits `BLOCK` — caching a timed-out
   reply would make blocking loops spin.
4. Its metadata proves it takes at least one key-name argument: a key spec without the `not_key`
   flag, or legacy metadata with `firstKey > 0` and `step > 0`.
5. It does **not** have the `nondeterministic_output` command tip.
6. It does **not** have the `script_runner` command flag (excludes `EVAL_RO`, `EVALSHA_RO`,
   `FCALL_RO`).

Unknown commands fail closed: no metadata means not cacheable. Module commands (`FT.*`, `TS.*`,
`JSON.*`, probabilistic types, vector sets) are evaluated by the same rules — there are no
prefix- or data-type-based exclusions.

Container subcommands declare their subcommand at construction
(`new CommandArguments(XINFO, STREAM)`) and are judged by their own `PARENT|CHILD` metadata — so
`XINFO STREAM` and `XINFO GROUPS` are cacheable while their sibling `XINFO CONSUMERS`
(nondeterministic output) is not, and `MEMORY USAGE` is cacheable. Commands built without a
declared subcommand keep the parent verdict, which is not cacheable.

## Overrides for known metadata gaps

These commands match the metadata rules but are denied through fixes applied to the shipped
metadata, because the server metadata is known to be incomplete. Each entry should be removed
once the corresponding server metadata is fixed (regeneration warns when an entry becomes
obsolete).

| Command | Override | Reason |
|---|---|---|
| `TOUCH` | adds `dont_cache` | Excluded by design: mutates key idle time, and its reply is an existence count aggregated across shards. |
| `VRANDMEMBER` | adds `nondeterministic_output` | Returns random elements, but unlike `SRANDMEMBER`, `ZRANDMEMBER` and `HRANDFIELD` it is not tagged. |
| `SORT_RO` | adds `dont_cache` | `BY`/`GET` pattern keys are external to the declared keys, so cached replies could never be invalidated when those keys change. |

`XREAD` no longer needs an override: the `blocking` flag rule excludes it.

## Changes from the hand-maintained allowlist (Jedis 8.0)

The metadata-derived policy changed some default verdicts compared to the allowlist shipped
through Jedis 8.0:

**No longer cached** (previously cacheable by default):

| Command | Reason |
|---|---|
| `TS.INFO` | carries the `dont_cache` command tip |
| `XPENDING` | carries the `nondeterministic_output` command tip |

**Newly cached by default** (previously never cached): commands whose metadata proves they are
read-only, keyed, and deterministic — including `SINTERCARD`, `ZDIFF`, `ZINTER`, `ZUNION`,
`ZINTERCARD`, `EXPIRETIME`/`PEXPIRETIME`, `HEXPIRETIME`/`HPEXPIRETIME`, `PFCOUNT`, `DIGEST`,
`FT.SUGGET`/`FT.SUGLEN`, the vector-set read commands (`VSIM`, `VCARD`, …), and the container
subcommands `XINFO STREAM`, `XINFO GROUPS`, and `MEMORY USAGE`.

If you relied on the old behavior, narrow the default set with
`CacheConfig.builder().excludeCommands(...)`.

The following APIs are deprecated in favor of the metadata-derived policy:

| Deprecated | Replacement |
|---|---|
| `DefaultCacheable` (class, `INSTANCE`, `isDefaultCacheableCommand`) | the built-in metadata-derived policy used automatically by `CacheConfig` |
| `Cacheable.isCacheable(ProtocolCommand, List)` | `Cacheable.isCacheable(CommandObject)` (default method; sees declared subcommands) |
| `CacheKey.getRedisCommand()` | `CacheKey.getCommandObject()` |
| `AllowAndDenyListWithStringKeys` | `CacheConfig.builder().excludeCommands(...)` |
| `CacheConfig.Builder.cacheable(...)` | `CacheConfig.builder().excludeCommands(...)` to narrow the default set by command, or `excludeCommands(Cacheable)` to narrow it per invocation (by keys) |

Existing `Cacheable` implementations keep working unchanged: the new `CommandObject` overload
delegates to the old signature by default, receiving the container command (e.g. `XINFO`) as
before. Override the new method if your policy should judge subcommands such as `XINFO STREAM`
individually.

## Regenerating

Command flags and cluster request/response policies are populated from the same metadata table
as cacheability, so regenerate it when targeting a new Redis release.

`CommandMetadataUtil` (test sources) fetches `COMMAND INFO` from a live server and produces both
`CommandMetadata.json` and the generated table of `MetadataResolver.java` (only the block between
the `GENERATED-METADATA-BEGIN` / `GENERATED-METADATA-END` markers is replaced). When the server is
unreachable, it falls back to the checked-in JSON and regenerates only the Java table. Mismatches
between the Jedis command set and the server metadata are printed as warnings, including metadata
fixes that have become obsolete.

Run from the repository root against a server with all modules loaded:

```sh
mvn -q test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=redis.clients.jedis.CommandMetadataUtil \
    -Dexec.args="-remote:localhost:6379"
```

Optional arguments: `-auth:password`, `-user:username`.

## External metadata file

Setting the `JEDIS_COMMAND_METADATA_PATH` environment variable to a JSON file with the
`CommandMetadata.json` layout makes Jedis load its command metadata from that file instead of the
built-in table — for example to apply metadata from a newer Redis release without rebuilding
Jedis. The file is read and parsed lazily, on first use, and kept once it loaded successfully. A
file that cannot be read is an error — an explicitly configured source is never silently replaced
by the built-in table. Any use of client-side caching (building a `CacheConfig`, creating a
cache, asking a policy for a verdict) or of the command-flags registry then fails with an
`IllegalStateException` carrying the load failure as its cause, so the application cannot keep
running with a wrong set of command information. The failure is not remembered: every attempt
reads the file again and fails the same way until the file is fixed. When the variable is not
set, the built-in metadata is used.

## Per-client command metadata

The environment variable above replaces the metadata process-wide. To configure metadata per
client instance instead — for example when two clients in the same application target servers
with different command sets — provide a `CommandMetadataResolver` through the client builder:

```java
CommandMetadataResolver resolver =
    CommandMetadataResolverFactory.fromFile(Paths.get("/path/to/CommandMetadata.json"));

RedisClient client = RedisClient.builder()
    .hostAndPort(node)
    .cacheConfig(CacheConfig.builder().build())
    .commandMetadataResolver(resolver)
    .build();
```

The file must have the `CommandMetadata.json` layout and is read eagerly — a missing or
malformed file fails the factory call. The resolver is instance-scoped: it affects only the
clients it is given to and is consulted by every metadata consumer of that client — client-side
caching derives its cacheability verdicts from it, and cluster clients build their command flags
registry (command flags and request/response policies) from it. Clients without a resolver keep
using the shared metadata. `CommandMetadataResolverFactory.defaultResolver()` returns the
resolver over the shared metadata, for delegation or explicit wiring; it is the builders'
default, and `commandMetadataResolver(null)` is rejected.

Resolvers compose with `withFallback(...)`: a command the first resolver does not know is looked
up in the fallback, for example a file that only lists the commands it changes, layered over the
shared metadata:

```java
CommandMetadataResolver resolver = CommandMetadataResolverFactory
    .fromFile(Paths.get("/path/to/overrides.json"))
    .withFallback(CommandMetadataResolverFactory.defaultResolver());
```

A fallback supplies metadata only. The eligibility rules still run on whatever metadata is
resolved, so a fallback cannot make a command cacheable that the rules deny. The first resolver
that knows a command supplies its complete metadata: once it knows a container command, that
command's subcommands are not looked up in the fallback.

### Customizing the eligibility decision with override records

To change the cacheability of single commands for one client, supply override records: metadata
records that correct one command, or describe a command the bundled metadata does not know. The
eligibility rules stay the same and run on the corrected metadata, so one record also corrects
the command flags and cluster policies derived from it.

```java
CommandMetadataResolver defaults = CommandMetadataResolverFactory.defaultResolver();

// correct an existing record: never cache GET for this client
CommandMetadata get = defaults.resolve("GET").toBuilder().tips("dont_cache").build();

// describe a command the bundled metadata does not know
CommandMetadata custom = CommandMetadata.builder("MYMODULE.READ")
    .flags("readonly").keyPositions(1, 1, 1).build();

RedisClient client = RedisClient.builder()
    .hostAndPort(node)
    .cacheConfig(CacheConfig.builder().build())
    .commandMetadataResolver(
        CommandMetadataResolverFactory.of(get, custom).withFallback(defaults))
    .build();
```

Override records are used as given; the known server metadata fixes are not applied to them. A
subcommand is corrected through its container record
(`defaults.resolve("XINFO").toBuilder().subcommand(...)`), because the first resolver that knows
a container supplies all of its subcommands. Keep override records narrow and remove them once
the server metadata is fixed.

The resolver applies to the cache the builder creates from `cacheConfig(...)`: the builder calls
`CacheFactory.createCache(CacheConfig, CommandMetadataResolver)` with the client's resolver (the
shared default when none is set), and the default policy, exclusions included, is derived from
that resolver. A custom `CacheFactory` set with `cacheFactory(...)` overrides that method and
receives the same two arguments. `CacheConfig.getCacheable()` is always the policy over the
shared default metadata, so a factory that wants the client's metadata applied should delegate
to `super.createCache(...)`. A ready-made cache passed to `cache(...)` keeps its own policy.

For metadata loaded from a file, the same preparation is applied as for the built-in metadata:
the known server metadata fixes are layered on top, and the loaded table is sealed against
modification. An explicitly set
`commandFlags(...)` registry on a cluster builder still takes precedence over the
resolver-derived default.

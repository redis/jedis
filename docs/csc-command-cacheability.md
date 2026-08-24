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
  command that is not cacheable by the default policy cannot be made cacheable. Excluding a
  container command excludes all of its subcommands.
- `withFallback(...)` supplies a `Cacheable` that decides commands with no metadata verdict (for
  example custom `ProtocolCommand` implementations). Without a fallback, such commands are not
  cacheable and logged once per command name.
- `cacheable(...)` replaces the policy with a user-provided `Cacheable`; it cannot be combined
  with exclusions or a fallback. For now the custom policy alone decides, preserving pre-8.1
  behavior; a later major release will let the default policy's denials take precedence. Note
  that `CacheConfig.getCacheable()` returns an internal wrapper in this case — do not rely on
  identity with the instance you supplied.

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
declared subcommand keep the parent verdict, which is not cacheable. Excluding a parent command
excludes all of its subcommands.

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

If you relied on the old behavior, configure explicit policies via
`CacheConfig.builder().excludeCommands(...)` (narrow the default set) or `cacheable(...)`
(replace it).

The following APIs are deprecated in favor of the metadata-derived policy:

| Deprecated | Replacement |
|---|---|
| `DefaultCacheable` (class, `INSTANCE`, `isDefaultCacheableCommand`) | the built-in metadata-derived policy used automatically by `CacheConfig` |
| `Cacheable.isCacheable(ProtocolCommand, List)` | `Cacheable.isCacheable(CommandObject)` (default method; sees declared subcommands) |
| `CacheKey.getRedisCommand()` | `CacheKey.getCommandObject()` |
| `AllowAndDenyListWithStringKeys` | `CacheConfig.builder().excludeCommands(...)` / a custom `Cacheable` |

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
Jedis. The file is read and parsed once, lazily, on first use. A file that cannot be read is an
error: it is logged with its cause and the command metadata is held as unavailable — an
explicitly configured source is never silently replaced by the built-in table. Any use of
client-side caching or the command-flags registry then fails with an `IllegalStateException`
carrying the original load failure as its cause, so the application cannot keep running with a
wrong set of command information. When the variable is not set, the built-in metadata is used.

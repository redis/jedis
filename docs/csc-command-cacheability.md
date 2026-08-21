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
  behavior; a later major release will let the default policy's denials take precedence.

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

`XREAD` no longer needs an override: the `blocking` flag rule excludes it.

## External metadata file

Setting the `JEDIS_COMMAND_METADATA_PATH` environment variable to a JSON file with the
`CommandMetadata.json` layout makes Jedis load its command metadata from that file instead of the
built-in table — for example to apply metadata from a newer Redis release without rebuilding
Jedis. The file is read and parsed once, lazily, on first use. A missing or malformed file fails
fast with an `IllegalStateException`: an explicitly configured metadata source is never silently
ignored. When the variable is not set, the built-in metadata is used.

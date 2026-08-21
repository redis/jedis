# Client-Side Caching: Command Cacheability

This page documents how Jedis decides which commands are eligible for client-side caching
(CSC). The rules are by design aligned across Redis client libraries, so clients given the same
command metadata make the same cacheability decision.

## Architecture

Command metadata and cacheability policy are separate components (all package-private in
`redis.clients.jedis.csc`):

- **`MetadataResolver`** holds normalized Redis `COMMAND` metadata (flags, tips, legacy key
  positions, key-spec key-name evidence) keyed by command name, **generated from a known Redis
  release** by `CommandMetadataUtil` — not curated by hand; do not edit the generated block.
  Container subcommands are keyed `PARENT|CHILD` (for example `MEMORY|USAGE`). An optional
  override table takes precedence over the generated metadata.
- **`CacheabilityResolver`** contains the eligibility rules and the known-server-metadata fixes
  listed below. It resolves the final verdict for every command known to Jedis — precedence,
  highest first: caller-supplied verdict overrides, metadata fixes, resolved metadata — and
  materializes the result into a `DefaultCacheable`.
- **`DefaultCacheable`** is the default policy: a map holding the final verdict per command.
  Commands missing from the map (no metadata, or a custom `ProtocolCommand`) are not cacheable
  and logged once per command name.
- **`CacheableWrapper`** wraps a user-provided `Cacheable` configured through
  `CacheConfig.Builder`, applying per-command verdict overrides with the same override-first
  approach.

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
(`new CommandArguments(XINFO, STREAM)`), and the arguments expose a pipe-merged effective command
(`getFullCommand()`, for example `XINFO|STREAM`) that the cache key reports and the resolver
judges by its own metadata — so `XINFO STREAM` and `XINFO GROUPS` are cacheable while their
sibling `XINFO CONSUMERS` (nondeterministic output) is not, and `MEMORY USAGE` is cacheable.
Commands built without a declared subcommand keep the parent verdict, which is not cacheable.
Excluding a parent command excludes all of its subcommands.

## Overrides for known metadata gaps

These commands match the metadata rules but are denied through metadata fixes built into
`MetadataResolver`, because the server metadata is known to be incomplete. Each entry should be
removed once the corresponding server metadata is fixed (the generator warns when an entry
becomes obsolete).

| Command | Override | Reason |
|---|---|---|
| `TOUCH` | adds `dont_cache` | Excluded by design: mutates key idle time, and its reply is an existence count aggregated across shards. |
| `VRANDMEMBER` | adds `nondeterministic_output` | Returns random elements, but unlike `SRANDMEMBER`, `ZRANDMEMBER` and `HRANDFIELD` it is not tagged. |

`XREAD` no longer needs an override: the `blocking` flag rule excludes it.

## Regenerating

`CommandMetadataUtil` (test sources) fetches `COMMAND INFO` from a live server and produces both
outputs:

- `CommandMetadata.json` in the repository root — the full normalized metadata, key specs included.
- The metadata table of `MetadataResolver.java` — the block between the
  `GENERATED-METADATA-BEGIN` / `GENERATED-METADATA-END` markers is replaced; everything else in
  the file is preserved.

Mismatches between the Jedis command set and the server metadata are printed as warnings: Jedis
`ProtocolCommand` constants with no `COMMAND` metadata (these fail closed at runtime), server
commands with no `ProtocolCommand` constant, and overrides that have become obsolete.

Run it from the repository root against a server with all modules loaded (the output tracks the
server it is pointed at):

```sh
mvn -q test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=redis.clients.jedis.csc.CommandMetadataUtil \
    -Dexec.args="-remote:localhost:6379"
```

All arguments are optional: `-remote:host:port` defaults to `localhost:6379`, and
`-auth:password` / `-user:username` authenticate the connection when needed. When the server is
unreachable, the tool falls back to the checked-in `CommandMetadata.json`: the file is left
untouched (it is the source) and only `MetadataResolver.java` is regenerated from it. The JSON
records its provenance — Redis version, mode, loaded modules, and generation time — so it is
always clear which server produced the metadata.

## External metadata file

Setting the `JEDIS_COMMAND_METADATA_PATH` environment variable to a JSON file with the
`CommandMetadata.json` layout makes `MetadataResolver` load its command table from that file instead
of the generated table — for example to apply metadata from a newer Redis release without
rebuilding Jedis. The file is read and parsed once, lazily, on first use, and shared by all
resolver instances. A missing or malformed file fails fast with an `IllegalStateException`: an
explicitly configured metadata source is never silently ignored. When the variable is not set,
the generated table is used.

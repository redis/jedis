# Hash Import (HIMPORT)

## Overview

The `HIMPORT` command (Redis 8.10 and later) is a fast way to create many hashes
that share a common set of field names — for example, importing a batch of records where every
record has the same columns. Rather than sending the field names again for every hash (as a
series of `HSET` calls would), the field names are declared once per connection and each hash
then supplies only its values, positionally matched to those fields.

On the wire this is a connection-local container command: `HIMPORT PREPARE` registers a named
field-set (the ordered field names) on the current connection, `HIMPORT SET` creates one hash
from a row of values against that field-set, and `HIMPORT DISCARD` releases it. A field-set
lives only on the connection that prepared it and disappears when that connection is reset or
closed.

Depending on the client, commands travel over a single direct connection (`Jedis`) or over a
pool of connections (`RedisClient`, `RedisClusterClient`) — with a pool, each `himportSet` can
end up on an arbitrary connection. Rather than expose the raw connection-local commands — which
are not safe to drive by hand across pooled connections — Jedis exposes a reusable `HashImport`
field-set template plus a per-row `himportSet` command, and manages the lifecycle: the
`HIMPORT PREPARE` is injected automatically the first time a template is seen on each
connection, a redirect to another cluster node simply re-prepares on demand, and closing the
template schedules `HIMPORT DISCARD` on every connection it touched. You never manage
`PREPARE`/`SET`/`DISCARD` ordering or connection pinning yourself.

> `HashImport` and `himportSet` are marked `@Experimental`: the API may change in a future
> release. They require Redis 8.10 or later.

## Usage

Create a field-set template once (declaring the shared field names, in order), then import each
hash by supplying its key and its values positionally against those fields:

```java
import redis.clients.jedis.HashImport;
import redis.clients.jedis.RedisClient;

RedisClient client = RedisClient.create("redis://localhost:6379");

// declare the field names shared by every hash we are importing; reusable and safe to share
try (HashImport fields = HashImport.of("name", "email", "age")) {

    // import as many hashes as you like — one command per hash, so the total is unbounded
    client.himportSet("user:1", fields, "alice", "a@example.com", "30");
    client.himportSet("user:2", fields, "bob",   "b@example.com", "25");
    client.himportSet("user:3", fields, "carol", "c@example.com", "42");
}
```

After these complete, `user:1`, `user:2` and `user:3` each exist as an ordinary hash with the
`name`, `email` and `age` fields set to their respective values — readable with `HGETALL`,
`HGET` and every other hash command. Any existing hash at an imported key is replaced.

Closing the template (`try`-with-resources or `close()`) schedules a best-effort
`HIMPORT DISCARD` to release the server-side state; it is optional (the state also dies with
the connection) but good hygiene for long-lived connections. A closed template must not be used
again.

## Pipelining

For large imports, combine the template with a pipeline to avoid a round trip per hash. The
`HIMPORT PREPARE` is buffered automatically ahead of the first use on the pipeline's
connection, and its reply is internal — `syncAndReturnAll()` returns exactly one result per
command you issued:

```java
import redis.clients.jedis.HashImport;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.Response;

RedisClient client = RedisClient.create("redis://localhost:6379");

try (HashImport fields = HashImport.of("name", "email", "age");
    Pipeline pipeline = client.pipelined()) {

    Response<String> u1 = pipeline.himportSet("user:1", fields, "alice", "a@example.com", "30");
    Response<String> u2 = pipeline.himportSet("user:2", fields, "bob",   "b@example.com", "25");
    Response<String> u3 = pipeline.himportSet("user:3", fields, "carol", "c@example.com", "42");

    pipeline.sync();

    // each returns "OK"
    u1.get();
    u2.get();
    u3.get();
}
```

## Cluster

`himportSet` works unchanged with `RedisClusterClient`: each key routes to its slot, and the
template re-prepares per node as needed.

## Limitations

- **Transactions**: `himportSet` is not supported inside `MULTI`/`EXEC` — the connection-local
  `PREPARE` cannot be staged in a transaction. Attempting it throws
  `UnsupportedOperationException`.
- **Cluster pipelines**: not supported. Use the standard (non-pipelined) `himportSet` on
  `RedisClusterClient` instead.
- A template dropped without `close()` leaves its server-side state on the connections it
  touched until those connections are recycled.
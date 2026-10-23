# Smart Client Handoffs (Maintenance Notifications)

## Overview

[Smart client handoffs (SCH)](https://redis.io/docs/latest/develop/clients/sch/) is a feature of
Redis Cloud and Redis Software servers that notifies clients about planned maintenance — shard
migrations, shard failovers, endpoint rebinds, and rolling upgrades — shortly before it happens, so
the client can respond gracefully instead of hitting command timeouts, dropped connections, and
`MOVED` redirects. The notifications arrive as RESP3 push messages, and Jedis reacts on the client
side:

- **Relaxed timeouts** — while a maintenance operation is in progress, socket read timeouts are
  temporarily loosened so in-flight and new commands survive the operation instead of timing out.
- **Pre-handoffs** (standalone) — when an endpoint moves (`MOVING`), the pool proactively directs
  new connections to the new endpoint and retires connections to the old one within the announced
  grace period, instead of waiting for a hard disconnect.
- **In-place slot map updates** (Redis Cluster) — when slots have moved (`SMIGRATED`), the client
  re-routes them to the new owner directly from the notification, without a `CLUSTER SLOTS` refresh
  and without a `MOVED` round trip.

The two deployment types receive different notification families, handled by different clients:

| Deployment | Client | Notifications | Reactions |
| --- | --- | --- | --- |
| Standalone endpoint (Redis Software / Redis Cloud database, single proxy endpoint) | `RedisClient` | `MIGRATING`, `MIGRATED`, `FAILING_OVER`, `FAILED_OVER`, `MOVING` | Relaxed timeouts per connection, pre-handoff on `MOVING` |
| OSS Cluster API (Redis Software / Redis Cloud database with the OSS cluster API enabled) | `RedisClusterClient` | `SMIGRATING`, `SMIGRATED` | Client-wide relaxed timeouts, in-place slot map update, pool removal for departed nodes |
| Several standalone endpoints with failover (see [Failover](failover.md)) | `MultiDbClient` | Standalone family, per database | As `RedisClient`, for each database that opts in; off by default |

The feature is negotiated per connection during the handshake with
`CLIENT MAINT_NOTIFICATIONS ON`. Servers that do not support the command are unaffected (see
[Modes](#modes)).

## Requirements

- **RESP3.** Push messages require the RESP3 protocol. On a RESP2 connection the feature stays
  inactive in `AUTO` mode and fails the connection setup in `ENABLED` mode.
- **Server support.** The server must support the `CLIENT MAINT_NOTIFICATIONS` command; see
  [SCH support in Redis server products](https://redis.io/docs/latest/develop/clients/sch/#sch-support-in-redis-server-products)
  for availability and enablement per product.
- **Client.** `RedisClient` (standalone) and `RedisClusterClient` (Redis Cluster) support the
  feature and enable it automatically on RESP3 connections. `MultiDbClient` supports the standalone
  family per database, opt-in. See [Standalone](#standalone) and [Redis Cluster](#redis-cluster)
  for the client-specific behavior.

!!! note "Legacy clients"
    The legacy `Jedis`, `JedisPool`, and `JedisCluster` clients do not negotiate maintenance
    notifications. Migrate to the builder-based `RedisClient` / `RedisClusterClient` to use the
    feature. Applications that construct a `PooledConnectionProvider` or a
    `ClusterConnectionProvider` directly can pass a `MaintenanceNotificationsConfig` to the
    provider constructor (since 8.1).

## Configuration

Both clients are configured through the same `MaintenanceNotificationsConfig`, passed to the
builder's `maintenanceNotifications(...)` method. All settings have defaults, so the feature works
out of the box on RESP3 connections.

| Setting | Default | Applies to | Description |
| --- | --- | --- | --- |
| `mode` | `AUTO` | both | Whether and how strictly the handshake is attempted; see [Modes](#modes). |
| `relaxedTimeout` | 10 s | both | Socket read timeout for non-blocking commands while a maintenance window is open, in milliseconds. |
| `relaxedBlockingTimeout` | `0` (infinite) | both | Socket read timeout for blocking commands (`BLPOP`, `XREAD BLOCK`, ...) while a window is open. |
| `relaxedWindowMaxDuration` | 60 s | both | Backstop after which a window opened by a start notification closes on its own if the matching end notification is lost. |
| `endpointType` | auto-resolved | standalone only | The address format requested for the `MOVING` target; see [Endpoint types](#endpoint-types). Has no effect on cluster connections. |

`MaintenanceNotificationsConfig.DISABLED` is a ready-made instance that turns the feature off.

### Modes

| Mode | Behavior |
| --- | --- |
| `AUTO` (default for `RedisClient` and `RedisClusterClient`) | The handshake is attempted on RESP3 connections; if the server rejects it (or the connection is RESP2), the feature is quietly disabled for that connection. |
| `ENABLED` | The handshake must succeed: connection setup fails if the server rejects `CLIENT MAINT_NOTIFICATIONS` or the connection is not RESP3. |
| `DISABLED` | The handshake is not attempted; the feature is off. |

In `AUTO` mode the decision is per connection: on a cluster where only some nodes support the
command, the connections to the other nodes simply run without notifications.

### Relaxed timeouts

While a maintenance window is open, the connection uses the **looser** of the relaxed value and the
configured socket timeout (`0` = infinite is the loosest) — relaxation never tightens a timeout.
Non-blocking and blocking commands have separate relaxed values (`relaxedTimeout` and
`relaxedBlockingTimeout`).

A window opened by a start notification (`MIGRATING`, `FAILING_OVER`, `SMIGRATING`) normally
closes with the matching end notification (`MIGRATED`, `FAILED_OVER`, `SMIGRATED`). If the end
notification is lost, the window reverts on its own after `relaxedWindowMaxDuration`. A `MOVING`
window has no closing notification: it ends when the announced grace period expires.

What a window covers differs between the two clients: on `RedisClient` it covers the connection
that received the notification (or, for `MOVING`, the pool), while on `RedisClusterClient` it
covers every connection of the client. See the client sections below.

## Standalone

`RedisClient` receives the standalone notification family. The server announces the operation on
every affected connection, and each connection reacts on its own.

### Enabling

Using RESP3 is enough — the default mode is `AUTO`:

```java
RedisClient client = RedisClient.builder()
    .hostAndPort(new HostAndPort("localhost", 6379))
    .clientConfig(DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3)
        .build())
    .build();
```

To customize the behavior, or turn the feature off:

```java
RedisClient client = RedisClient.builder()
    .hostAndPort(new HostAndPort("localhost", 6379))
    .clientConfig(DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3)
        .build())
    .maintenanceNotifications(MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED) // fail setup if unsupported
        .relaxedTimeout(30_000)                            // non-blocking commands, millis
        .relaxedBlockingTimeout(0)                         // blocking commands; 0 = infinite
        .build())
    .build();

// off:
RedisClient plain = RedisClient.builder()
    .hostAndPort(new HostAndPort("localhost", 6379))
    .maintenanceNotifications(MaintenanceNotificationsConfig.DISABLED)
    .build();
```

### Notifications and how the client reacts

| Notification | Wire format | Meaning | Client reaction |
| --- | --- | --- | --- |
| `MIGRATING` | `["MIGRATING", seq, startsInSeconds, shard-ids]` | A shard migration starts within the lead time. | Relax the receiving connection's timeouts. |
| `MIGRATED` | `["MIGRATED", seq, shard-ids]` | The migration completed. | Restore the receiving connection's timeouts. |
| `FAILING_OVER` | `["FAILING_OVER", seq, startsInSeconds, shard-ids]` | A shard failover starts within the lead time. | Relax the receiving connection's timeouts. |
| `FAILED_OVER` | `["FAILED_OVER", seq, shard-ids]` | The failover completed. | Restore the receiving connection's timeouts. |
| `MOVING` | `["MOVING", seq, graceSeconds, "host:port"]` | The endpoint moves to the target; connections to the old endpoint hard-disconnect after the grace period. | Relax timeouts pool-wide, direct new connections to the target, and retire connections to the old endpoint (see below). |

A `MIGRATING`/`FAILING_OVER` window relaxes only the connection that received it. A `MOVING`
window relaxes every pool connection toward the affected endpoint until the grace period expires.

### Pre-handoff (`MOVING`)

During the grace period:

- Commands in flight on existing connections complete normally under the relaxed timeouts.
- New connections — including replacements for returned ones — are opened against the endpoint
  carried by the notification and inherit the relaxed timeouts.
- Connections to the old endpoint are retired: each is discarded when returned to the pool.
- A connection held past the grace period is disconnected by the server; using it throws
  `JedisConnectionException`, and returning it replaces it with a fresh connection.

#### Endpoint types

The client tells the server which address format to return in `MOVING`. By default this is
auto-resolved per connection: a private remote IP requests `INTERNAL_*`, a public one `EXTERNAL_*`;
TLS requests `*_FQDN`, plaintext `*_IP`. A fixed type can be requested with
`MaintenanceNotificationsConfig.builder().endpointType(...)`, including `NONE` — the notification
then carries no target and the client schedules a reconnect to its configured endpoint at half the
grace period.

When choosing a fixed type, make sure the requested address family is routable from where the
client runs — an internal IP is typically not reachable from outside the cluster's network.

See the [official SCH documentation](https://redis.io/docs/latest/develop/clients/sch/) for the
cross-client overview, server-side enablement, and product-specific limitations.

### `MultiDbClient`

A `MultiDbClient` holds one connection pool per database, and each database reacts to the
standalone family on its own. The feature is **off by default** there; opt in per database through
`DatabaseConfig`:

```java
HostAndPort east = new HostAndPort("redis-east.example.com", 6379);
DatabaseConfig primary = DatabaseConfig.builder(east, clientConfig)
    .weight(1.0f)
    .maintenanceNotificationsConfig(MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.AUTO)
        .build())
    .build();

MultiDbClient client = MultiDbClient.builder()
    .multiDbConfig(MultiDbConfig.builder().database(primary).build())
    .build();
```

Maintenance notifications do not trigger a failover themselves. Relaxed timeouts and pre-handoff
help the active database stay usable through the operation, so fewer failures reach the circuit
breaker and an unnecessary switch to another database is less likely.

### Pub/Sub

A subscribed connection consumes maintenance notifications inline between pub/sub messages, so
relaxed timeouts apply to it like to any other connection. Pre-handoff, however, does not: the
subscribed connection is held by the listen loop and cannot be replaced underneath the
application, so on `MOVING` it stays on the old endpoint and is disconnected by the server when
the grace period expires. The subscription then fails with `JedisConnectionException`;
re-subscribing — on a fresh connection, which reaches the new endpoint — is the application's
responsibility.

## Redis Cluster

`RedisClusterClient` receives the cluster notification family, which describes planned slot
movements between nodes. Behind the OSS cluster API, a shard failover, a slot migration, a node
replacement, and a rolling upgrade all look the same to the client: a set of slots changes owner,
possibly moving to a node that was not part of the topology before, possibly leaving a node with
no slots at all. The server announces the start and the end of such an operation on **every**
connection to **every** node, and Jedis folds those copies into a single client-wide reaction.

### Enabling

As for `RedisClient`, using RESP3 is enough — the default mode is `AUTO`:

```java
RedisClusterClient cluster = RedisClusterClient.builder()
    .nodes(Collections.singleton(new HostAndPort("localhost", 7000)))
    .clientConfig(DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3)
        .build())
    .build();
```

To customize the behavior, or turn the feature off — the configuration object and the
[modes](#modes) are the same as for `RedisClient`:

```java
RedisClusterClient cluster = RedisClusterClient.builder()
    .nodes(Collections.singleton(new HostAndPort("localhost", 7000)))
    .clientConfig(DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3)
        .build())
    .maintenanceNotifications(MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED) // fail setup if a node rejects it
        .relaxedTimeout(30_000)                            // non-blocking commands, millis
        .relaxedBlockingTimeout(0)                         // blocking commands; 0 = infinite
        .build())
    .build();

// off:
RedisClusterClient plain = RedisClusterClient.builder()
    .nodes(Collections.singleton(new HostAndPort("localhost", 7000)))
    .maintenanceNotifications(MaintenanceNotificationsConfig.DISABLED)
    .build();
```

In `ENABLED` mode a connection whose node rejects the handshake (or that ends up on RESP2) is not
pooled, and the command that needed it fails with `JedisClusterOperationException` once the
configured `maxAttempts` are exhausted.

### Notifications and how the client reacts

| Notification | Wire format | Meaning | Client reaction |
| --- | --- | --- | --- |
| `SMIGRATING` | `["SMIGRATING", seq, "slots"]` | The listed slots or ranges (e.g. `"123,456,789-1000"`) start moving. | Open a client-wide relax window: timeouts are relaxed on **all** cluster connections. |
| `SMIGRATED` | `["SMIGRATED", seq, [["src", "dst", "slots"], ...]]` | The listed slots moved from node `src` to node `dst` (bare `host:port` addresses, one entry per source/destination pair). | Re-route the slots to `dst` in place, drop the pool of a source node left without slots, and close the relax window. |

Every connection to every node receives its own copy of each notification. The copies carry the
same sequence number, and Jedis applies each notification exactly once per client regardless of
how many connections deliver it. An `SMIGRATED` with an empty list of entries is valid: it only
closes the relax window.

On the wire, `SMIGRATING` is a three-element push frame and `SMIGRATED` carries a nested array:

```text
>3
+SMIGRATING
:93923
+123,456,789-1000

>3
+SMIGRATED
:93924
*2
  *3
    +node-a.example.com:6789
    +node-b.example.com:6790
    +123,789-1000
  *3
    +node-a.example.com:4545
    +node-c.example.com:4555
    +1000-2000
```

### What a maintenance operation looks like

Take a rolling upgrade in which node A is replaced by a new node A':

1. The server adds A' to the cluster. The client does not notice yet: A' owns no slots.
2. Every node sends `SMIGRATING` with the slots that are about to move. The client opens the
   relax window, and from now on every command on every connection runs under the relaxed
   timeouts — including commands on connections that are opened while the window is open, from
   their handshake onwards.
3. The server moves the slots to A' and, during the move, keeps serving the moving slots from A
   behind the scenes, so the client sees stalls at worst, not errors. The relaxed timeouts absorb
   the stalls.
4. Every node sends `SMIGRATED` listing the moved slots with A as source and A' as destination.
   The first copy to arrive updates the slot map: a connection pool for A' is created, the slots
   are re-routed to it, and since A no longer owns any slot its pool is destroyed. The relax
   window closes and timeouts return to normal.
5. Commands for the moved slots go straight to A'. No `MOVED` redirect, no `CLUSTER SLOTS`
   refresh, and no reconnect storm is needed.
6. The server removes A from the cluster. The client has no connection to it anymore.

A shard failover or a plain slot migration between existing nodes follows the same steps, except
that no pool is created or destroyed: the destination node is already known, and the source node
keeps its other slots.

### Relaxed timeouts are client-wide

- An `SMIGRATING` received on **any** connection relaxes the timeouts of **every** connection of
  the client, not just the one that received it, because a slot move behind the OSS cluster API
  can stall traffic on nodes other than the moving ones.
- Connections opened while a window is open are relaxed from their first byte: the handshake
  itself (`HELLO`, `AUTH`, `CLIENT MAINT_NOTIFICATIONS`) runs under the relaxed timeout.
- Several operations can overlap. Each `SMIGRATED` closes one window, and timeouts return to
  normal only when the last window has closed. The timeouts also stay relaxed until the slot
  delta of the closing `SMIGRATED` has actually been applied, so there is no gap in which a
  command could time out between the notification and the routing update.
- A window whose `SMIGRATED` is lost is closed by the next `SMIGRATED`, or by the
  `relaxedWindowMaxDuration` backstop (default 60 s), whichever comes first.

### Slot map updated in place

Applying an `SMIGRATED` rewrites the client's slot table directly from the notification:

- **Destination nodes** that were unknown so far get a connection pool created on the spot.
- **Source nodes** left with nothing to serve — no primary slot owned and no replica slot used
  for read-from-replica — have their connection pool destroyed, and the node is forgotten. A
  source node that keeps other slots is left alone.
- **Topology refreshes still happen** and take precedence. A delta that arrives while a
  `CLUSTER SLOTS` refresh is in flight (periodic, or triggered by a `MOVED`) is queued and applied
  right after the refresh, so the routing table always ends up reflecting the newer state.
- **Client-side caching** is flushed when a delta is applied, exactly as on a full refresh, so no
  value cached from the old owner is served after the move.
- **Self-healing.** If notifications arrive out of order and a slot is briefly routed to the wrong
  node, the usual `MOVED` handling and the following topology refresh correct it. Keep
  `maxAttempts` at its default (or above 1) so such a redirect is followed transparently.

### Differences from the standalone behavior

- **No pre-handoff.** `MOVING` is not part of the cluster family. Nodes are not re-mapped and
  connections are not retired per pool; a source node that leaves the cluster has its whole
  connection pool dropped once the slot delta has been applied. Consequently the endpoint-type
  settings described under [Pre-handoff](#pre-handoff-moving) only apply to `MOVING` and have no
  effect on cluster connections.
- **Standalone notifications are ignored.** A `MOVING`, `MIGRATING`, `MIGRATED`, `FAILING_OVER`,
  or `FAILED_OVER` push arriving on a cluster connection is logged at `WARN` level and otherwise
  ignored; it indicates a database that is not running in OSS cluster mode.
- **One window per client, not per connection.** See
  [Relaxed timeouts are client-wide](#relaxed-timeouts-are-client-wide).

### Pub/Sub

A sharded pub/sub connection (`ssubscribe`) consumes `SMIGRATING` and `SMIGRATED` inline between
pub/sub messages, so a notification received on it opens or closes the client-wide window exactly
like one received on a command connection, and messages published before and after the move are
delivered in order. Jedis does not move a live subscription, though: if the slot of a subscribed
channel moves to another node, or the node the subscription is connected to leaves the cluster,
the server ends the subscription (typically by closing the connection, which fails the listen loop
with `JedisConnectionException`). Re-subscribing — on a fresh connection, which the updated slot
map routes to the new owner — is the application's responsibility.

### Observability

The cluster reaction is logged through SLF4J at `DEBUG` level:

| Logger | Logs |
| --- | --- |
| `redis.clients.jedis.ClusterMaintenanceCoordinator` | Each `SMIGRATING` that opens a window (slots and sequence number), each `SMIGRATED` that closes one, and the connection it arrived on. |
| `redis.clients.jedis.JedisClusterInfoCache` | Nodes removed after a slot migration. |
| `redis.clients.jedis.ClusterMaintenanceController` | Standalone-family notifications received on a cluster connection (`WARN`). |

To confirm that the handshake took place on a connection, run `CLIENT MAINT_NOTIFICATIONS_INFO`
on it (available on servers that support the feature).

## Production notes

- **JVM DNS caching (standalone).** After the grace period of a `MOVING`, the client reconnects
  using the configured endpoint name. The JVM caches successful DNS lookups for a fixed 30 seconds
  by default (ignoring record TTLs), which can outlive the grace period. Disable the cache or set
  it to a low value via the `networkaddress.cache.ttl`
  [security property](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/jvm-ttl-dns.html).
- **Keep retries on (cluster).** Maintenance notifications remove `MOVED` redirects from the
  common path, but they are an optimization on top of the regular cluster redirect handling, not a
  replacement for it. Leave `maxAttempts` at its default so the rare redirect — a notification
  lost on a flapping connection, or a node that does not support the handshake — is still followed
  transparently.
- **Relaxed timeout versus failure detection.** While a window is open, a genuinely dead
  connection takes up to `relaxedTimeout` (and up to `relaxedBlockingTimeout`, infinite by default,
  for blocking commands) to be reported. Size the relaxed values to the longest stall you are
  willing to wait through during maintenance, and set `relaxedBlockingTimeout` to a finite value
  if your blocking commands must fail within a bound.

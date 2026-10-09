# Smart Client Handoffs (Maintenance Notifications)

## Overview

[Smart client handoffs (SCH)](https://redis.io/docs/latest/develop/clients/sch/) is a feature of
Redis Cloud and Redis Software servers that notifies clients about planned maintenance — shard
migrations, shard failovers, and endpoint rebinds — shortly before it happens, so the client can
respond gracefully instead of hitting command timeouts and dropped connections. The notifications
arrive as RESP3 push messages, and Jedis reacts on the client side:

- **Relaxed timeouts** — while a maintenance operation is in progress, socket read timeouts are
  temporarily loosened so in-flight and new commands survive the operation instead of timing out.
- **Pre-handoffs** — when an endpoint moves (`MOVING`), the pool proactively directs new
  connections to the new endpoint and retires connections to the old one within the announced grace
  period, instead of waiting for a hard disconnect.

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
  feature and enable it automatically on RESP3 connections. The two clients receive different
  notification families; see [Redis Cluster](#redis-cluster) for the cluster-specific behavior.

## Enabling

With `RedisClient`, using RESP3 is enough — the default mode is `AUTO`:

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

### Modes

| Mode | Behavior |
| --- | --- |
| `AUTO` (default for `RedisClient` and `RedisClusterClient`) | The handshake is attempted on RESP3 connections; if the server rejects it (or the connection is RESP2), the feature is quietly disabled for that connection. |
| `ENABLED` | The handshake must succeed: connection setup fails if the server rejects `CLIENT MAINT_NOTIFICATIONS` or the connection is not RESP3. |
| `DISABLED` | The handshake is not attempted; the feature is off. |

## Notifications and how the client reacts

| Notification | Wire format | Meaning | Client reaction |
| --- | --- | --- | --- |
| `MIGRATING` | `["MIGRATING", seq, startsInSeconds, shard-ids]` | A shard migration starts within the lead time. | Relax the receiving connection's timeouts. |
| `MIGRATED` | `["MIGRATED", seq, shard-ids]` | The migration completed. | Restore the receiving connection's timeouts. |
| `FAILING_OVER` | `["FAILING_OVER", seq, startsInSeconds, shard-ids]` | A shard failover starts within the lead time. | Relax the receiving connection's timeouts. |
| `FAILED_OVER` | `["FAILED_OVER", seq, shard-ids]` | The failover completed. | Restore the receiving connection's timeouts. |
| `MOVING` | `["MOVING", seq, graceSeconds, "host:port"]` | The endpoint moves to the target; connections to the old endpoint hard-disconnect after the grace period. | Relax timeouts pool-wide, direct new connections to the target, and retire connections to the old endpoint (see below). |

## Relaxed timeouts

While a maintenance window is open, the connection uses the **looser** of the relaxed value and the
configured socket timeout (`0` = infinite is the loosest) — relaxation never tightens a timeout.
Non-blocking and blocking commands have separate relaxed values (`relaxedTimeout`, default 10 s, and
`relaxedBlockingTimeout`, default `0` = infinite).

A window opened by `MIGRATING`/`FAILING_OVER` normally closes with the matching
`MIGRATED`/`FAILED_OVER`. If the closing notification is lost, the window reverts on its own after
`relaxedWindowMaxDuration` (default 60 s). A `MOVING` window has no closing notification: relaxation
applies to every pool connection toward the affected endpoint and ends when the announced grace
period expires.

## Pre-handoff (`MOVING`)

During the grace period:

- Commands in flight on existing connections complete normally under the relaxed timeouts.
- New connections — including replacements for returned ones — are opened against the endpoint
  carried by the notification and inherit the relaxed timeouts.
- Connections to the old endpoint are retired: each is discarded when returned to the pool.
- A connection held past the grace period is disconnected by the server; using it throws
  `JedisConnectionException`, and returning it replaces it with a fresh connection.

### Endpoint types

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

## Redis Cluster

`RedisClusterClient` receives a separate notification family for slot migrations. The server
announces the start and end of a migration on every connection to the node, and Jedis folds those
copies into a single client-wide reaction:

| Notification | Wire format | Meaning | Client reaction |
| --- | --- | --- | --- |
| `SMIGRATING` | `["SMIGRATING", seq, "slots"]` | The listed slots or ranges (e.g. `"123,456,789-1000"`) start migrating. | Relax timeouts on **all** cluster connections until the migration ends. |
| `SMIGRATED` | `["SMIGRATED", seq, [["src", "dst", "slots"], ...]]` | The listed slots moved from `src` to `dst`. | Re-route the slots to `dst` in place, without a full `CLUSTER SLOTS` refresh; close the relax window; drop the pool of a source node left without slots. |

Differences from the standalone behavior:

- **Relaxed timeouts are client-wide.** Timeouts stay relaxed on every connection while any
  migration is open, and return to normal only when the last one closes (or after
  `relaxedWindowMaxDuration` if the closing notification is lost).
- **No pre-handoff.** `MOVING` is not part of the cluster family; nodes are not re-mapped and
  connections are not retired per pool. A source node that owns no slots after the migration has its
  connection pool removed once the slot delta has been applied.
- **Slot deltas never fight a topology refresh.** A delta that arrives while a full refresh is in
  flight is queued and applied right after it, so the routing table always reflects the newer state.
  Client-side caching is flushed when a delta is applied, as it is on a full refresh.

Enable, tune, or turn the feature off through the cluster builder — the configuration object and the
[modes](#modes) are the same as for `RedisClient`:

```java
RedisClusterClient cluster = RedisClusterClient.builder()
    .nodes(Collections.singleton(new HostAndPort("localhost", 7000)))
    .clientConfig(DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3)
        .build())
    .maintenanceNotifications(MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED)
        .relaxedTimeout(30_000)
        .build())
    .build();

// off:
RedisClusterClient plain = RedisClusterClient.builder()
    .nodes(Collections.singleton(new HostAndPort("localhost", 7000)))
    .maintenanceNotifications(MaintenanceNotificationsConfig.DISABLED)
    .build();
```

The endpoint-type settings described under [Pre-handoff](#pre-handoff-moving) only apply to
`MOVING` and have no effect on cluster connections.

## Pub/Sub

A subscribed connection consumes maintenance notifications inline between pub/sub messages, so
relaxed timeouts apply to it like to any other connection. Pre-handoff, however, does not: the
subscribed connection is held by the listen loop and cannot be replaced underneath the
application, so on `MOVING` it stays on the old endpoint and is disconnected by the server when
the grace period expires. The subscription then fails with `JedisConnectionException`;
re-subscribing — on a fresh connection, which reaches the new endpoint — is the application's
responsibility.

## Production notes

- **JVM DNS caching.** After the grace period the client reconnects using the configured endpoint
  name. The JVM caches successful DNS lookups for a fixed 30 seconds by default (ignoring record
  TTLs), which can outlive the grace period. Disable the cache or set it to a low value via the
  `networkaddress.cache.ttl`
  [security property](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/jvm-ttl-dns.html).

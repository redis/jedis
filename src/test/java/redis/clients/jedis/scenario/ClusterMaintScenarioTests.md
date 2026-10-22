# Cluster maintenance notifications (SMIGRATING / SMIGRATED) scenario tests

Scenario tests for the OSS-cluster maintenance notification support in `RedisClusterClient`, run
against a real Redis Enterprise cluster whose slot moves are driven by the fault injector (FI).
They complement the mock tests in [`sch/ClusterMaintenanceIT.md`](../sch/ClusterMaintenanceIT.md):
the mocks pin the mechanics frame by frame; these tests confirm the same reactions against the
real notification stream, real `CLUSTER SLOTS` output, real timing and real node identities.

Test code refers to the case ids below (`S1.1`, `S3.4`, ...) in observation names and javadoc, and
to the mock cases (`C2`, `C4`, `F2`, ...) of the mock plan where a scenario case confirms one.

| Section | Topic | Test class |
|---|---|---|
| [S1](#s1-reception-and-payload) | Reception and payload | [`ClusterMaintNotificationsIT`](ClusterMaintNotificationsIT.java) |
| [S2](#s2-relaxed-timeouts) | Relaxed timeouts, client-wide | [`ClusterMaintNotificationsIT`](ClusterMaintNotificationsIT.java) (S2.1, S2.2); [`ClusterMaintNotificationsTimeoutIT`](ClusterMaintNotificationsTimeoutIT.java) (S2.3, S2.4) |
| [S3](#s3-slot-map-and-pools) | Slot map and pools follow the deltas | [`ClusterMaintNotificationsIT`](ClusterMaintNotificationsIT.java) (S3.1 to S3.5); [`ClusterMaintNotificationsFeatureInteractionIT`](ClusterMaintNotificationsFeatureInteractionIT.java) (S3.6 to S3.8) |
| [S4](#s4-connections-opened-during-a-window) | Connections opened during a window | [`ClusterMaintNotificationsTimeoutIT`](ClusterMaintNotificationsTimeoutIT.java) |
| [S5](#s5-traffic-continuity) | Traffic continuity, black box | [`ClusterMaintenanceTrafficResilienceIT`](ClusterMaintenanceTrafficResilienceIT.java) |
| [S6](#s6-tls) | TLS | [`ClusterMaintNotificationsTlsIT`](ClusterMaintNotificationsTlsIT.java) |
| [S8](#s8-configuration-and-handshake) | Configuration and handshake | [`ClusterMaintNotificationsConfigIT`](ClusterMaintNotificationsConfigIT.java) |
| [S9](#s9-pubsub) | Sharded pub/sub across a migration | [`ClusterMaintNotificationsFeatureInteractionIT`](ClusterMaintNotificationsFeatureInteractionIT.java) |
| [S10](#s10-lifecycle) | Lifecycle | [`ClusterMaintNotificationsIT`](ClusterMaintNotificationsIT.java) |
| [E1](#e1-replica-exposure-environment-check) | Replica exposure (environment check) | [`ClusterReplicaExposureIT`](ClusterReplicaExposureIT.java) |

(S7, endpoint-type variants, is not a section of its own: it is the `ip` / `hostname` column of
the [chosen combinations](#chosen-effect-x-trigger-x-variant-combinations).)

## Chosen (Effect X Trigger X Variant) combinations

[`ClusterMaintNotificationsIT.effectTriggerCombinations()`](ClusterMaintNotificationsIT.java):
both Case 2 directions (endpoint set changes) on every trigger, plus the endpoint-stable Case 1 on
the trigger with the shortest window. `hostname` runs where the node identity carried by
`SMIGRATED` matters most, one non-ASM and one ASM row; `ip` elsewhere. Every row must be offered
by the FI: a missing trigger or variant fails the method rather than shrinking the matrix
silently; `-Dsch.skipAsm=true` is the one explicit way to skip the ASM rows.

| Row | Effect | Trigger | Variant | Endpoint delta | What it isolates |
|---|---|---|---|---|---|
| 1 | `remove-add` | `migrate` | `external/hostname` | -1 / +1 | C2 pool dropped and C4 pool created in one delta; longest window |
| 2 | `slot-shuffle` | `failover` | `external/ip` | 0 | Case 1 with the most nodes; sub-second open / close |
| 3 | `add` | `migrate` | `external/ip` | +1 | C4 alone: a pool for a destination never seen in `CLUSTER SLOTS` |
| 4 | `remove` | `failover` | `external/ip` | -1 | C2 alone on the fast trigger |
| 5 | `add` | `reshard` (ASM) | `external/ip+asm` | +1 | Scale-out cascade; the case behind the settle rule |
| 6 | `remove` | `reshard` (ASM) | `external/hostname+asm` | -1 | Scale-in cascade with pool destruction under repeated windows |

`maintenance_mode` and `internal` variants are not run.

## Test cases

Each row of `migrationIsHandled(scenario)` takes every applicable case below on one database and
one effect firing. "Expected" is what the client must do; the server's message contract is only
asserted where a client requirement rests on it.

### S1. Reception and payload

| Id | Expected |
|---|---|
| S1.1 | At least one pinned connection received `SMIGRATING` and at least one received `SMIGRATED`. Whether every connection receives them is not the client's concern; per-connection counts are logged. Where several connections recorded the same `(type, seq)`, the client applied it once (S3.1 holds). |
| S1.2 | Every recorded event parsed into a usable payload: a slot set on both message types, `src != dest` on every `SMIGRATED` entry. |
| S1.3 | `SMIGRATED.src` is a node key the client had before the effect and `SMIGRATED.dest` is one it has after, in the same `host:port` form the client keys its pools by (identity consistency between `CLUSTER SLOTS` and `SMIGRATED`; checked on the hostname rows above all). A mismatch prints both forms: it would need a client-side normalisation, a feature change, not a test change. |

### S2. Relaxed timeouts

| Id | Expected | Where |
|---|---|---|
| S2.1 | Right after the first `SMIGRATING` is read on any one pinned connection, and before any other pinned connection reads anything: `hasActiveMigration` is true, at least one window is open, and every pinned connection is relaxed (`isRelaxedTimeoutActive`, effective timeout 30 000; both read without a round-trip), including those whose own push is still unread (named in the observation; only the gate can have relaxed them). A window that closed before it could be observed (`SMIGRATED` buffered behind the same reply as the first `SMIGRATING`) fails the row: it is asserted on every row, never skipped. Mock B1 to B4. | `migrationIsHandled`, step 3 |
| S2.2 | After the settle rule: no active migration, zero open windows, and every surviving pinned connection is back at the 1000 ms base on its next command. | `migrationIsHandled`, after step 5 |
| S2.3 | Behavioural proof of relaxation: a blocking command with a 3 s server-side delay on a slot that is not moving succeeds inside a long window and times out at the 2 s blocking base after it. Needs a window longer than the probe (`remove-add` / `migrate`); the window must be open when the probe starts (record and gate), asserted, never skipped. A warm-up command precedes each probe so connection creation stays out of the timing. | `ClusterMaintNotificationsTimeoutIT.blockingCommandSucceedsInsideTheWindow` |
| S2.4 | Relaxation never tightens: a client configured with a 60 000 ms socket timeout keeps it before, during and after the window. The gate must still be open right after the "during" value is read, asserted, never skipped. Mock B7. | `ClusterMaintNotificationsTimeoutIT.relaxationNeverTightensALargerTimeout` |

### S3. Slot map and pools

| Id | Expected | Where |
|---|---|---|
| S3.1 | For every delta of every `SMIGRATED`, after the settle rule: `slotOwner(slot) == dest` for each slot in the range, and the owner of every slot not named in any delta is unchanged. Mock C1. | `migrationIsHandled` |
| S3.2 | `slot-shuffle`: the node set equals the one before the effect and no pool was closed. Mock C3. | row 2 |
| S3.3 | `add` and the added side of `remove-add`: the node set gained exactly one key, equal to a `dest`; its pool carries the cluster maintenance controller and serves a PING. Mock C4. | rows 1, 3, 5 |
| S3.4 | `remove` and the removed side of `remove-add`: the node set lost exactly one key, equal to a `src` that owns no slot afterwards; its pool is closed; the pinned connection to it completed the PING that carried the push and returns to its destroyed pool without throwing. Mock C2, F2. | rows 1, 4, 6 |
| S3.5 | What the application sees after the settle rule: through the observer (`maxAttempts(1)`), one `SET` + `GET` per moved range and per surviving node's unmoved range each return the value. Zero `MOVED`, zero failed connections, zero other exceptions, asserted separately so the report names the kind. A `MOVED` here means the slot map was not applied (S3.1), a failed connection that a pool is unusable (S3.3); the DMC's post-`SMIGRATED` forwarding grace can hide such a defect but never cause one, so the case does not depend on it. The verification client's node set must equal the observer's. | `migrationIsHandled`, step 6 |
| S3.6 | Three migrations on one client, one database and one set of pinned connections: each run's deltas are applied on top of the previous state (S3.1), the node set never changes (S3.2), no window is left open after each run, and the `seq` a connection reads only ever increases. | `ClusterMaintNotificationsFeatureInteractionIT.repeatedMigrationsOnOneClient` |
| S3.7 | Periodic topology refresh (2 s) interleaving with deltas: every `SET` / `GET` issued during the effect through the `maxAttempts(1)` observer succeeds (one attempt, no `MOVED`, no failed connection), and the final owners equal a fresh `CLUSTER SLOTS`. Mock C8, C11. | `ClusterMaintNotificationsFeatureInteractionIT.deltasInterleaveWithPeriodicRefresh` |
| S3.8 | Client-side cache: a `GET` cached before the move (a hit on the second read) is gone after the delta: the cache's flush count rose, its size is 0, and the next read is a cache miss served by the server. Mock C9. | `ClusterMaintNotificationsFeatureInteractionIT.clientSideCacheIsFlushedByADelta` |

### S4. Connections opened during a window

| Id | Expected | Where |
|---|---|---|
| S4.1 | Connections created while a window is open are relaxed from their first command (handshake under the gate), including one to the departing node, which after `SMIGRATED` returns to its destroyed pool without throwing. Whether the server also sends `SMIGRATING` to the late connection is logged. The borrow necessarily creates a connection: the only pooled connection ever returned idle before it is the seed node's discovery connection, which the pinned borrow took. The window must be open at the borrow, asserted, never skipped. Mock A6, F3. | `ClusterMaintNotificationsTimeoutIT.connectionsOpenedDuringTheWindow`, `remove-add` / `migrate` |

### S5. Traffic continuity

| Id | Expected | Where |
|---|---|---|
| S5.1 | A stock `RedisClusterClient` (RESP3, AUTO mode, default retries) with worker threads doing `SET` / `GET` on one key per shard (see [Fixture](#fixture)) across the effect sees zero command errors and every `GET` returns what the preceding `SET` wrote (a stale or null read is a failure); every key written exists afterwards; no worker left in flight. Rows: `remove-add` / `migrate`, `slot-shuffle` / `failover`, `remove` / `reshard`. | `ClusterMaintenanceTrafficResilienceIT.trafficContinues` |
| S5.2 | The same traffic through a `maxAttempts(1)` client on the same database: what an application without retries sees. Every failure is classified by the key's slot (moved or not, from the owner map before and after the effect), by whether the slot's owner before the effect is still a node afterwards, and by its cause. Two kinds are accepted and only counted: a borrow from the destroyed pool of a departed node for a key in a moved slot (all rows), and a `MOVED` for a key in a moved slot on ASM rows (the shard answers the redirect itself at the switch instant). Any other failure fails the row: timeouts, connections cut by the server, `MOVED` on unmoved slots or on non-ASM rows, destroyed-pool borrows on unmoved slots. | same method |
| S5.3 | Negative control (opt-in `-Dsch.negativeControl=true`): traffic with notifications disabled and `maxAttempts(1)` across `remove-add` / `migrate` is expected to see failures, showing what the notifications bought. | `ClusterMaintenanceTrafficResilienceIT.trafficDisruptedWithoutMaintenance` |

### S6. TLS

| Id | Expected | Where |
|---|---|---|
| S6.1 | S1.1, S1.3, S2.1, S2.2, S3.1, S3.3, S3.4 hold over `rediss://` with the truststore pinned to the server chain and default hostname verification, on `remove-add` / `migrate` with the `hostname` variant (the FQDN in `SMIGRATED` is verified against the new node's certificate). The class inherits `migrationIsHandled` from `ClusterMaintNotificationsIT` and supplies the one TLS row as its parameter source. | `ClusterMaintNotificationsTlsIT` |

### S8. Configuration and handshake

| Id | Expected | Where |
|---|---|---|
| S8.1 | ENABLED and AUTO over RESP3: `CLIENT MAINT_NOTIFICATIONS_INFO` reports `on` on every node; every pool has the cluster maintenance controller. | `ClusterMaintNotificationsConfigIT.enabledAndAutoActivateNotificationsOnEveryNode` |
| S8.2 | DISABLED: reports `off` on every node; no pool has a controller. | `ClusterMaintNotificationsConfigIT.disabledDoesNotActivateNotificationsAnywhere` |
| S8.3 | DISABLED during a real migration: no push dispatched to its connections, timeouts unchanged, `SET` / `GET` on one key per shard (see [Fixture](#fixture)) still succeed through the stock `MOVED` handling and refresh. | `ClusterMaintNotificationsConfigIT.disabledClientIsUnaffectedByARealMigration` |
| S8.4 | AUTO over RESP2: no push consumer on any connection, commands work. | `ClusterMaintNotificationsConfigIT.autoOverResp2SendsNoHandshakeAndWorks` |
| S8.5 | ENABLED over RESP2: fails at build or first command with a `JedisException`. | `ClusterMaintNotificationsConfigIT.enabledOverResp2Fails` |

### S9. Pub/Sub

| Id | Expected | Where |
|---|---|---|
| S9.1 | One `JedisShardedPubSub` subscription per shard (see [Fixture](#fixture)), each on its own recorded observer connection. Every channel whose slot did not move receives `before` and `after` in order and keeps its connection; at least one subscribed connection consumed `SMIGRATING` and `SMIGRATED` inline; the client-wide gate is seen open during the effect and no window is left after it. A heartbeat per channel per second (published from the verification client) keeps the subscriptions inside the 2 s blocking timeout and is excluded from the assertions. Mock G1. | `ClusterMaintNotificationsFeatureInteractionIT.shardedPubSubSurvivesAMigration` |
| S9.2 | The one subscribed channel in the moved shard is the single casualty, asserted as such: the server closes its subscribed connection at or before `SMIGRATED` (observed 2026-10-20 on RE: `Unexpected end of stream`, `SMIGRATING` recorded on it but no `SMIGRATED`), Jedis has no resubscription for application pub/sub, so that channel got `before` only, its subscriber ended with a `JedisConnectionException`, exactly one channel sat in the moved range, and it is the only one that missed `after`. If the server starts keeping the connection or sending `sunsubscribe`, or Jedis gains resubscription, these observations fail and name which changed. | same method |

### S10. Lifecycle

| Id | Expected | Where |
|---|---|---|
| S10 | `client.close()` after the effect closes every node pool, including the pools the deltas had already dropped; pinned connections were returned first; the database is released only after every client is closed. Mock F1. | `migrationIsHandled`, step 7 |

### E1. Replica exposure (environment check)

Not a maintenance-notification case: it pins down what the RE OSS Cluster API tells a cluster
client about replicas, so the suite's assumptions about node sets (every pool is a master) rest on
an asserted fact rather than on folklore. No effect is fired.

| Id | Expected | Where |
|---|---|---|
| E1 | On a replicated database (`slot-shuffle` / `failover`, 6 shards, replication on), the same `CLUSTER SLOTS` the client's discovery sends, issued on every node the client found, covers all 16384 slots with one node entry per range and no replica entries; `CLUSTER SHARDS` lists only `role=master` and `CLUSTER NODES` only master flags; `INFO replication` on each node still reports `role:master` with `connected_slaves` above zero (the replicas exist behind the proxy). The client's node set equals the masters named by `CLUSTER SLOTS`, and a client built with `readOnlyForRedisClusterReplicas()` has no pool beyond them (its replica reads fall back to the master). If RE starts exposing replicas, the first assertion fails and prints the entries. | `ClusterReplicaExposureIT.replicasAreNotExposedByTheClusterApi` |

## Vocabulary

- **Effect**: the client-observable outcome of a slot move, as the FI names it
  ([`SlotMigrateEffect`](../../../../com/redis/test/fi/SlotMigrateEffect.java)). `slot-shuffle`:
  slots move between nodes that keep owning slots, endpoint set unchanged. `add`: one shard moves
  to an empty node, one endpoint added. `remove`: all shards leave a node for existing nodes, one
  endpoint removed. `remove-add`: all shards leave a node for an empty node, one removed and one
  added.
- **Trigger**: the server mechanism producing the effect. `migrate` (`rladmin migrate`),
  `failover` (master / replica role swap, needs replication), `reshard` (Atomic Slot Migration,
  ASM; a cascade of short windows driven by the decision engine, provisioned through the FI's
  `/slot-migrate/setup` and released with `/teardown`).
- **Variant** ([`Requirement.config()`](../../../../com/redis/test/fi/Requirement.java)): the
  database configuration the FI offers for a trigger, named `external/ip`, `external/hostname`,
  with `+tls` and `+asm` suffixes. `ip` / `hostname` is the database's
  `oss_cluster_api_preferred_endpoint_type`: the form of the addresses in `CLUSTER SLOTS` and in
  `SMIGRATED`.
- **Row**: one effect x trigger x variant combination; one database, one effect firing, one run
  of a parameterised test method.
- **Window**: the time between the first `SMIGRATING` read by the client and the matching
  `SMIGRATED` being applied. While a window is open the client-wide **gate** relaxes the socket
  timeouts of every connection of the client, not only the one that read the push.
- **Delta**: one `SMIGRATED` entry `(src, dest, slots)`: those slots now route to `dest`; a `src`
  left without slots is dropped together with its pool.
- **Observer**: the `RedisClusterClient` under test, configured so the behaviour is observable
  (see [Fixture](#fixture)). **Verification client**: a second client with maintenance
  notifications disabled, used only to read the server-side truth.

## Fixture

Provided by [`ClusterMaintNotificationsScenarioBase`](ClusterMaintNotificationsScenarioBase.java)
on top of [`FaultInjectorScenarioBase`](FaultInjectorScenarioBase.java):

- **Database per test.** `deployDatabase(scenario)` provisions the row's database from a fresh FI
  discovery (`create_database`, or `/slot-migrate/setup` for an ASM row) and the base's
  `@AfterEach` releases it after every client is closed. Expectations are never constants: the
  node count and the slot layout are read from the live cluster, so the tests run unchanged on a
  3-node or a 6-node cluster.
- **Observer client.** RESP3; `socketTimeoutMillis` 1000, `blockingSocketTimeoutMillis` 2000,
  `relaxedTimeout` and `relaxedBlockingTimeout` 30 000 (the standalone observer's constants, so
  relaxed and restored timeouts are directly observable); `maxAttempts(1)` so every redirect and
  failed connection surfaces to the test as an exception it can count instead of a silent retry;
  no periodic topology refresh; pool `maxTotal` 3 per node. TLS trust is pinned to the chain the
  database presents when the row is a TLS one; the chain is captured once per database and the
  truststore shared by every client the test opens on it.
- **Shard ranges.** `shardRanges(scenario, client)` computes one `[from, to]` range per shard from
  the database's `shards_count` with RE's even split (boundary `i` at `round(i * 16384 / n)`) and
  fails the test if the client's owner table changes owner at any other slot. The server cannot be
  asked: on RE both `CLUSTER SLOTS` and `CLUSTER SHARDS` report contiguous ranges per node, so
  adjacent shards owned by one node appear as one entry (probed 2026-10-22: six single-shard
  entries, then one `rladmin migrate` onto a neighbour's node made them five).
- **Keys.** [`ClusterKeyRegistry`](../util/ClusterKeyRegistry.java) hands out `{tag}:name` keys:
  the registered tag pins the slot, the caller's name keeps keys of different tests and workers in
  the same slot apart. `keysAcrossRanges(name, ranges)` gives one such key per shard.
- **Traffic.** [`TrafficRunner`](TrafficRunner.java) workers issue `SET` then `GET` on their keys
  and check every `GET` against the value the preceding accepted `SET` wrote; a mismatch is a
  `StaleReadException`, recorded like any other failure.
- **Shared database.** Classes whose methods fire on one database (`provisionShared`) release it
  in `@AfterAll`; if the endpoint never becomes connectable the database is released before the
  error propagates, so nothing is left on the FI.
- **One pinned connection per node.** `pinOneConnectionPerNode()` borrows a connection from every
  node's pool and keeps it, each with a `ReceivedEvents` recorder
  ([`MaintNotificationsTestSupport`](../MaintNotificationsTestSupport.java)). Pushes are only
  consumed on a read, so `observeAll()` PINGs each pinned connection; `awaitFirstSMigrating()`
  PINGs them one at a time and stops at the first that recorded `SMIGRATING`, leaving the others
  unread for the S2.1 gate check. A pinned connection the server closes is recorded in `dropped`;
  that is expected only for a departing node after its `SMIGRATED` was read somewhere, and fails
  the test otherwise.
- **Effect on a background thread.** `startEffect(scenario)` fires the FI route and awaits the
  action on its own thread; `effectFailures` collects FI errors for `assertEffectSucceeded()`.
- **Settle rule.** `awaitEffectSettled(expectedNodes)` ends a row in three steps that must stay
  in this order: the FI action completed (authoritative end of the cascade), then the pinned
  connections were pumped until no event arrived for 3 s, then the client's node count reached
  `before + endpointDelta(effect)`. The node count alone is not a stop condition: during an ASM
  scale-out the new node gets its first shard early and more deltas follow, so a count-only poll
  would race the unrelax assertions. Expected `SMIGRATED` counts are never asserted.
- **Observations.** Assertion failures are collected per named check and rethrown together at the
  end of the method (`Observations`), so one failing observation does not hide the independent
  ones taken on the same effect. Gates that make the rest meaningless still fail fast through
  plain assertions: the pre-effect snapshot match, no `SMIGRATING` within budget, FI action
  failure, convergence timeout.
- **Budgets.** First `SMIGRATING` within 150 s (ASM plans before the first slot moves); settle
  quiet 3 s; node-set convergence 120 s; FI effect 180 s, 600 s for ASM; `@Timeout(900)` per row.

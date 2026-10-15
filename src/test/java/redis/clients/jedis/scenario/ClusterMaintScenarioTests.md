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
| [S2](#s2-relaxed-timeouts) | Relaxed timeouts, client-wide | [`ClusterMaintNotificationsIT`](ClusterMaintNotificationsIT.java) (S2.1, S2.2); S2.3, S2.4 planned |
| [S3](#s3-slot-map-and-pools) | Slot map and pools follow the deltas | [`ClusterMaintNotificationsIT`](ClusterMaintNotificationsIT.java) (S3.1 to S3.5); S3.6 to S3.8 planned |
| [S4](#s4-connections-opened-during-a-window) | Connections opened during a window | planned |
| [S5](#s5-traffic-continuity) | Traffic continuity, black box | planned |
| [S6](#s6-tls) | TLS | planned |
| [S8](#s8-configuration-and-handshake) | Configuration and handshake | planned |
| [S9](#s9-pubsub) | Sharded pub/sub across a migration | planned |
| [S10](#s10-lifecycle) | Lifecycle | [`ClusterMaintNotificationsIT`](ClusterMaintNotificationsIT.java) |

(S7, endpoint-type variants, is not a section of its own: it is the `ip` / `hostname` column of
the [chosen combinations](#chosen-effect-x-trigger-x-variant-combinations).)

## Chosen (Effect X Trigger X Variant) combinations

[`ClusterMaintNotificationsIT.effectTriggerCombinations()`](ClusterMaintNotificationsIT.java):
both Case 2 directions (endpoint set changes) on every trigger, plus the endpoint-stable Case 1 on
the trigger with the shortest window. `hostname` runs where the node identity carried by
`SMIGRATED` matters most, one non-ASM and one ASM row; `ip` elsewhere. An ASM row is left out
with a warning when the FI does not offer `reshard`; `-Dsch.skipAsm=true` skips ASM rows.

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
| S2.1 | Right after the first `SMIGRATING` is read on any one pinned connection, every pinned connection is relaxed (`isRelaxedTimeoutActive`, effective timeout 30 000) after its next PING, including nodes whose own push has not been read yet. Mock B1 to B4. | `migrationIsHandled`, step 3 |
| S2.2 | After the settle rule: no active migration, zero open windows, and every surviving pinned connection is back at the 1000 ms base on its next command. | `migrationIsHandled`, after step 5 |
| S2.3 | Behavioural proof of relaxation: a blocking command with a 3 s server-side delay on a slot that is not moving succeeds inside a long window and times out at the 2 s blocking base after it. Needs a window longer than the probe (`remove-add` / `migrate`). | planned, `ClusterMaintNotificationsTimeoutIT` |
| S2.4 | Relaxation never tightens: a client configured with a 60 000 ms socket timeout keeps it during the window. Mock B7. | planned, `ClusterMaintNotificationsTimeoutIT` |

### S3. Slot map and pools

| Id | Expected | Where |
|---|---|---|
| S3.1 | For every delta of every `SMIGRATED`, after the settle rule: `slotOwner(slot) == dest` for each slot in the range, and the owner of every slot not named in any delta is unchanged. Mock C1. | `migrationIsHandled` |
| S3.2 | `slot-shuffle`: the node set equals the one before the effect and no pool was closed. Mock C3. | row 2 |
| S3.3 | `add` and the added side of `remove-add`: the node set gained exactly one key, equal to a `dest`; its pool carries the cluster maintenance controller and serves a PING. Mock C4. | rows 1, 3, 5 |
| S3.4 | `remove` and the removed side of `remove-add`: the node set lost exactly one key, equal to a `src` that owns no slot afterwards; its pool is closed; the pinned connection to it completed the PING that carried the push and returns to its destroyed pool without throwing. Mock C2, F2. | rows 1, 4, 6 |
| S3.5 | What the application sees, with no assumption about the DMC's post-`SMIGRATED` forwarding grace: through the observer (`maxAttempts(1)`), one `SET` + `GET` per moved range and per surviving node's unmoved range each reach a definitive outcome, a value or a visible `JedisClusterOperationException` / connection exception, never a hang. The counts of `MOVED`-driven exceptions and failed connections are logged, not asserted; only "unexpected exception type" fails. The verification client's node set must equal the observer's. | `migrationIsHandled`, step 6 |
| S3.6 | Repeated migrations on one client and one database: `seq` monotonic per connection across runs, S3.1 / S3.2 hold after each, windows back to zero each time. | planned, `ClusterMaintNotificationsFeatureInteractionIT` |
| S3.7 | Periodic topology refresh (2 s) interleaving with deltas: every command gets a definitive outcome, final owners equal a fresh `CLUSTER SLOTS`. Mock C8, C11. | planned, same class |
| S3.8 | Client-side cache: a value cached before the move is re-read from the server after the delta. Mock C9. | planned, same class |

### S4. Connections opened during a window

| Id | Expected | Where |
|---|---|---|
| S4.1 | Connections created while a window is open are relaxed from their first command (handshake under the gate), including one to the departing node, which after `SMIGRATED` returns to its destroyed pool without throwing. Whether the server also sends `SMIGRATING` to the late connection is logged. Mock A6, F3. | planned, `ClusterMaintNotificationsTimeoutIT`, `remove-add` / `migrate` |

### S5. Traffic continuity

| Id | Expected | Where |
|---|---|---|
| S5.1 | A stock `RedisClusterClient` (AUTO mode, default retries) with worker threads doing `SET` / `GET` over every shard across the effect sees zero command errors; every key written is readable afterwards; no worker left in flight. | planned, `ClusterMaintenanceTrafficResilienceIT` |
| S5.2 | The same traffic through a `maxAttempts(1)` client reports how many commands the application would have seen fail without retries. Report only. | planned, same class |
| S5.3 | Negative control (opt-in `-Dsch.negativeControl=true`): traffic with notifications disabled and `maxAttempts(1)` is expected to see `MOVED` failures, showing what the notifications bought. | planned, same class |

### S6. TLS

| Id | Expected | Where |
|---|---|---|
| S6.1 | S1.1, S1.3, S2.1, S2.2, S3.1, S3.3, S3.4 hold over `rediss://` with the truststore pinned to the server chain and default hostname verification, on `remove-add` / `migrate` with the `hostname` variant (the FQDN in `SMIGRATED` is verified against the new node's certificate). | planned, `ClusterMaintNotificationsTlsIT` |

### S8. Configuration and handshake

| Id | Expected | Where |
|---|---|---|
| S8.1 | ENABLED and AUTO over RESP3: `CLIENT MAINT_NOTIFICATIONS_INFO` reports enabled on every node; every pool has the cluster maintenance controller. | planned, `ClusterMaintNotificationsConfigIT` |
| S8.2 | DISABLED: reports disabled on every node; no pool has a controller. | planned, same class |
| S8.3 | DISABLED during a real migration: no event recorded, timeouts unchanged, commands still succeed through `MOVED` handling and refresh. | planned, same class |
| S8.4 | AUTO over RESP2: no handshake, commands work. | planned, same class |
| S8.5 | ENABLED over RESP2: fails at build or first command with a clear exception. | planned, same class |

### S9. Pub/Sub

| Id | Expected | Where |
|---|---|---|
| S9.1 | A `JedisShardedPubSub` subscription on channels whose slots do not move consumes `SMIGRATING` / `SMIGRATED` inline; messages published before and after the move are delivered in order; the client-wide gate opens from the subscribed connection's push. Channel selection pins the FI's `source_node` and subscribes one channel per shard of that node plus one elsewhere, so which channels are non-moving is known after the fact. Mock G1. | planned, `ClusterMaintNotificationsFeatureInteractionIT` |
| S9.2 | The one subscribed channel that turned out to be in the moved shard: behaviour documented, no assertion until the contract is agreed. | planned, same class |

### S10. Lifecycle

| Id | Expected | Where |
|---|---|---|
| S10 | `client.close()` after the effect closes every node pool; pinned connections were returned first; the database is released only after every client is closed. Mock F1. | `migrationIsHandled`, step 7 |

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
  database presents when the row is a TLS one.
- **One pinned connection per node.** `pinOneConnectionPerNode()` borrows a connection from every
  node's pool and keeps it, each with a `ReceivedEvents` recorder
  ([`MaintNotificationsTestSupport`](../MaintNotificationsTestSupport.java)). Pushes are only
  consumed on a read, so `observeAll()` PINGs each pinned connection. A pinned connection the
  server closes is recorded in `dropped`; that is expected only for a departing node after its
  `SMIGRATED` was read somewhere, and fails the test otherwise.
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

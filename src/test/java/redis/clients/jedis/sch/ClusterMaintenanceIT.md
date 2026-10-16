# Cluster maintenance (SMIGRATING / SMIGRATED) mock test plan

Test cases for the cluster maintenance notification support, run against a three-node
`TcpMockServer` cluster. Each section below is implemented by one `*MockTest` class in this
package; every test method links back to its row here.

| Section | Topic | Test class |
|---|---|---|
| [A](#a-handshake-and-per-connection-wiring) | Handshake and per-connection wiring | [`ClusterMaintenanceHandshakeMockTest`](ClusterMaintenanceHandshakeMockTest.java) |
| [B](#b-client-wide-relaxed-timeouts) | Client-wide relaxed timeouts | [`ClusterRelaxedTimeoutMockTest`](ClusterRelaxedTimeoutMockTest.java) |
| [C](#c-slot-delta-application) | Slot delta application | [`ClusterSlotMigrationMockTest`](ClusterSlotMigrationMockTest.java) |
| [D](#d-standalone-family-on-a-cluster-connection) | Standalone / enterprise family on a cluster connection | [`ClusterStandaloneEventsIgnoredMockTest`](ClusterStandaloneEventsIgnoredMockTest.java) |
| [E](#e-builder-and-provider-surface) | Builder and provider surface | [`ClusterMaintenanceHandshakeMockTest`](ClusterMaintenanceHandshakeMockTest.java) |
| [F](#f-lifecycle) | Lifecycle | [`ClusterSlotMigrationMockTest`](ClusterSlotMigrationMockTest.java) |
| [G](#g-pubsub) | Pub/Sub | [`ClusterMaintenancePubSubMockTest`](ClusterMaintenancePubSubMockTest.java) |

## Fixture

Shared by every test through [`AbstractClusterMaintenanceMockTest`](AbstractClusterMaintenanceMockTest.java):

- **Three mock nodes.** A owns slots 0–8191, B owns 8192–16383, C owns nothing until a test moves
  slots to it. Each node is a `TcpMockServer` driven by a `ClusterSlotsResponder` over one shared
  `MockClusterTopology`. The `RedisClusterClient` is bootstrapped from A.
- **Timeouts.** Baseline socket timeout 2000 ms (blocking 1000 ms); relaxed 10000 ms
  (blocking 4000 ms).
- **Push delivery.** A push is written by the server immediately, but the client only reads it on
  its next read of that connection. `deliver(server, slot, push)` borrows the connection for a
  slot, pushes, and pings so the push is consumed on a known connection.
- **Slot moves.** A move is first applied to the shared topology (the server side completed the
  migration) and then announced with the push. A client that ignored the push would be redirected
  with `MOVED` and show up in the `CLUSTER SLOTS` counters; a client that applied the delta in
  place shows unchanged counters.

## A. Handshake and per-connection wiring

`MaintenanceAwareVisitor` + `ClusterMaintenanceController`. Implemented in
[`ClusterMaintenanceHandshakeMockTest`](ClusterMaintenanceHandshakeMockTest.java).

| ID | Scenario | Expected | Test |
|---|---|---|---|
| A1 | AUTO over RESP3, server accepts | Each node saw exactly one `CLIENT MAINT_NOTIFICATIONS ON moving-endpoint-type internal-ip` per connection; each pooled connection has the pool controller as listener, the client-wide relax overlay, no per-connection expiring overlay, and a maintenance push consumer | [`autoOverResp3WiresEveryPooledConnection`](ClusterMaintenanceHandshakeMockTest.java#L41) |
| A2 | AUTO over RESP3, server rejects `CLIENT MAINT_NOTIFICATIONS` | Connections to that node work and carry no listener / overlay / consumer; connections to other nodes are wired; no exception | [`autoWithARejectingNodeIsQuietlyOffForThatNodeOnly`](ClusterMaintenanceHandshakeMockTest.java#L58) |
| A3 / E3 | ENABLED, server rejects | The first command routed to that node fails with `JedisClusterOperationException`; no half-wired connection stays in the pool; other nodes unaffected | [`enabledWithARejectingNodeFailsTheCommandAndKeepsNoConnection`](ClusterMaintenanceHandshakeMockTest.java#L73) |
| A4 | AUTO over RESP2 | No handshake sent; no overlay / listener / consumer on the connection (the pool still has its controller) | [`resp2SkipsTheHandshakeAndInstallsNothing`](ClusterMaintenanceHandshakeMockTest.java#L90) |
| A5 / E2 | DISABLED | No handshake on any node; no pool has a controller | [`disabledSendsNoHandshakeAndWiresNoController`](ClusterMaintenanceHandshakeMockTest.java#L101) |
| A6 | Connection opened while a migration window is open | Its handshake reads are relaxed from before `HELLO`: a `HELLO` slower than the socket timeout succeeds; without the window the same setup fails (control) | [`connectionOpenedDuringAnOpenWindowRelaxesItsHandshake`](ClusterMaintenanceHandshakeMockTest.java#L116) |

## B. Client-wide relaxed timeouts

Implemented in [`ClusterRelaxedTimeoutMockTest`](ClusterRelaxedTimeoutMockTest.java).

| ID | Scenario | Expected | Test |
|---|---|---|---|
| B1 | SMIGRATING read on a connection to A | That connection's socket `SO_TIMEOUT` becomes the relaxed value | [`sMigratingRelaxesTheReceivingConnection`](ClusterRelaxedTimeoutMockTest.java#L35) |
| B2 | Then a connection to B issues a command | Relaxed too: the gate is client-wide, B never received a push | [`relaxationIsClientWideAndACloserOnAnyConnectionRevertsAll`](ClusterRelaxedTimeoutMockTest.java#L51) |
| B3 | SMIGRATED read on B only | Every connection reverts to the baseline on its next command | [`relaxationIsClientWideAndACloserOnAnyConnectionRevertsAll`](ClusterRelaxedTimeoutMockTest.java#L51) |
| B4 | Two open windows; copies of one SMIGRATED on three connections to one node, the same closer delivered a second time, then a distinct closer, then copies of an already seen SMIGRATING | Only the first copy of the closer concludes a window, the later copies and the second delivery leave the second window open (a re-applied copy would have closed it too); the distinct closer ends it; a copy read on one connection opens or closes the gate for connections that have not read theirs yet; the re-delivered opener does not reopen; nothing throws | [`copiesOfOnePairOnSeveralConnectionsOpenAndCloseOnce`](ClusterRelaxedTimeoutMockTest.java#L75) |
| B5 | SMIGRATING without a closer, clock advanced past `relaxedWindowMaxDuration` | Next command runs at the baseline again | [`windowWithALostCloserRevertsAtTheTtlBackstop`](ClusterRelaxedTimeoutMockTest.java#L149) |
| B6 | `BLPOP` whose reply takes longer than the configured blocking timeout but less than the relaxed one | Succeeds while a window is open; after the closer the same delay times out (control) | [`relaxedBlockingTimeoutAppliesDuringABlockingCommand`](ClusterRelaxedTimeoutMockTest.java#L171) |
| B7 | Configured timeout larger than the relaxed one, or infinite | Relaxation never tightens: 20000 stays 20000, 0 stays 0 | [`relaxationNeverTightensALargerConfiguredTimeout`](ClusterRelaxedTimeoutMockTest.java#L187), [`relaxationKeepsAnInfiniteConfiguredTimeout`](ClusterRelaxedTimeoutMockTest.java#L201) |
| B8 | Relaxed while a delta waits behind a refresh | Covered together with C8 in [`ClusterSlotMigrationMockTest`](ClusterSlotMigrationMockTest.java) | [`deltaArrivingDuringARefreshIsAppliedAfterItAndRelaxesMeanwhile`](ClusterSlotMigrationMockTest.java#L182) |

## C. Slot delta application

`JedisClusterInfoCache` through real pools. Implemented in [`ClusterSlotMigrationMockTest`](ClusterSlotMigrationMockTest.java).

| ID | Scenario | Expected | Test |
|---|---|---|---|
| C1 | SMIGRATED `[[A, B, "0-8191"]]` after the server side moved the range | The next command for the range reaches B; no `CLUSTER SLOTS`, no `MOVED` | [`sMigratedReroutesInPlaceAndDropsTheDepartedSource`](ClusterSlotMigrationMockTest.java#L42) |
| C2 | Source left without slots | A disappears from the node map, its pool is closed, the server sees its connections go | [`sMigratedReroutesInPlaceAndDropsTheDepartedSource`](ClusterSlotMigrationMockTest.java#L42) |
| C3 | Source still owning slots | A stays; moved slots route to B, the rest to A | [`sourceStillOwningSlotsIsKept`](ClusterSlotMigrationMockTest.java#L69) |
| C4 | Destination never seen in `CLUSTER SLOTS` | A pool for C is created on demand; its connections get the handshake and the client-wide overlay | [`neverSeenDestinationGetsAPoolAndTheHandshake`](ClusterSlotMigrationMockTest.java#L90) |
| C5 | SMIGRATED without entries | A valid closer: the relax window ends; no slot is rerouted, no pool is dropped, no refresh | [`emptySMigratedOnlyEndsTheRelaxWindow`](ClusterSlotMigrationMockTest.java#L138) |
| C6 | Copies of one SMIGRATED on three connections to A | Applied once; A's pool destroyed once; no error | [`duplicateClosersOnSeveralConnectionsApplyOnce`](ClusterSlotMigrationMockTest.java#L112) |
| C7 | Malformed frames: unparseable slots, invalid port, inverted range | Dropped; the connection stays usable; topology and timeouts unchanged | [`malformedClusterFramesAreDroppedAndTheConnectionStaysUsable`](ClusterSlotMigrationMockTest.java#L161) |
| C8 / B8 | Delta arriving while a refresh holds the slot table | Connections are relaxed while the delta is pending; after the (stale) refresh the delta is applied on top | [`deltaArrivingDuringARefreshIsAppliedAfterItAndRelaxesMeanwhile`](ClusterSlotMigrationMockTest.java#L182) |
| C9 | Client-side cache enabled | A delta flushes the cache: the next read goes to the server even though the key's slot did not move | [`clientSideCacheIsFlushedWhenADeltaIsApplied`](ClusterSlotMigrationMockTest.java#L220) |
| C10 | Opener delivered, closer lost, server side moved | The client self-heals through `MOVED` + refresh; the window still expires on the TTL backstop | [`lostCloserSelfHealsThroughMovedAndTheTtlBackstop`](ClusterSlotMigrationMockTest.java#L241) |
| C11 | Periodic topology refresh running while deltas keep arriving | Every command lands on the current owner, exactly one attempt each, no exception | [`deltasAndPeriodicRefreshesConverge`](ClusterSlotMigrationMockTest.java#L270) |

## D. Standalone family on a cluster connection

The standalone / enterprise push family (`MOVING`, `MIGRATING`, `MIGRATED`, `FAILING_OVER`,
`FAILED_OVER`) is inert on cluster connections. Implemented in
[`ClusterStandaloneEventsIgnoredMockTest`](ClusterStandaloneEventsIgnoredMockTest.java).

| ID | Scenario | Expected | Test |
|---|---|---|---|
| D1 | `MOVING`, `MIGRATING`, `MIGRATED`, `FAILING_OVER`, `FAILED_OVER` pushed on a connection to A | Logged and ignored: no relaxation, no retirement, no remap (new connections still go to A, never to the MOVING target), topology unchanged, no refresh | [`standaloneFamilyPushesAreInertOnClusterConnections`](ClusterStandaloneEventsIgnoredMockTest.java#L28) |

## E. Builder and provider surface

Implemented in [`ClusterMaintenanceHandshakeMockTest`](ClusterMaintenanceHandshakeMockTest.java).

| ID | Scenario | Expected | Test |
|---|---|---|---|
| E1 | `RedisClusterClient.builder()` default | Handshake sent, cluster controller on every pool, and the controller's configured mode is AUTO (the first two alone would also hold under ENABLED) | [`builderDefaultsToAutoHandshake`](ClusterMaintenanceHandshakeMockTest.java#L135) |
| E2 | `.maintenanceNotifications(DISABLED)` | See A5 | [`disabledSendsNoHandshakeAndWiresNoController`](ClusterMaintenanceHandshakeMockTest.java#L101) |
| E3 | `.maintenanceNotifications(ENABLED)` + rejecting server | See A3 | [`enabledWithARejectingNodeFailsTheCommandAndKeepsNoConnection`](ClusterMaintenanceHandshakeMockTest.java#L73) |
| E4 | `new ClusterConnectionProvider(..., maintConfig)` directly | Node pools have the cluster controller; the 5-argument constructor leaves them without one | [`providerConstructorWiresTheClusterControllerOnlyWhenConfigured`](ClusterMaintenanceHandshakeMockTest.java#L148) |

## F. Lifecycle

Implemented in [`ClusterSlotMigrationMockTest`](ClusterSlotMigrationMockTest.java).

| ID | Scenario | Expected | Test |
|---|---|---|---|
| F1 | `client.close()` | Every node pool closed; all server-side connections gone | [`closingTheClientClosesEveryPoolAndConnection`](ClusterSlotMigrationMockTest.java#L306) |
| F2 | Pool destroyed by a delta while one of its connections is borrowed | The in-flight command completes, returning the connection does not throw, the connection is not reused | [`sMigratedReroutesInPlaceAndDropsTheDepartedSource`](ClusterSlotMigrationMockTest.java#L42) |
| F3 | Server drops all connections during an open window | Reconnected connections re-handshake and are relaxed immediately | [`connectionsReopenedDuringAnOpenWindowAreRelaxedFromTheHandshake`](ClusterSlotMigrationMockTest.java#L325) |

## G. Pub/Sub

Implemented in [`ClusterMaintenancePubSubMockTest`](ClusterMaintenancePubSubMockTest.java).

| ID | Scenario | Expected | Test |
|---|---|---|---|
| G1 | A sharded subscription on a connection to A receives SMIGRATING between two `smessage` frames | The maintenance push is consumed inline (the client-wide gate opens for every other connection); both messages are delivered, in order | [`subscribedConnectionConsumesMaintenancePushesInline`](ClusterMaintenancePubSubMockTest.java#L31) |

package com.redis.test.fi;

/**
 * Client-observable outcomes of the fault injector's {@code /slot-migrate} route on an OSS-cluster
 * database: how the hash-slot map and the endpoint set change. Each effect is produced by one or
 * more named triggers; see {@link FaultInjectorClient#getSlotMigrateTriggers(SlotMigrateEffect)}.
 */
public enum SlotMigrateEffect implements EffectType {

  /** Slots move between nodes that keep owning slots; the endpoint set is unchanged. */
  SLOT_SHUFFLE("slot-shuffle"),
  /** One shard moves to a previously empty node: one endpoint added. */
  ADD("add"),
  /** All shards leave a node for an existing node: one endpoint removed. */
  REMOVE("remove"),
  /** All shards leave a node for a previously empty node: one endpoint removed, one added. */
  REMOVE_ADD("remove-add");

  private final String wireName;

  SlotMigrateEffect(String wireName) {
    this.wireName = wireName;
  }

  @Override
  public String wireName() {
    return wireName;
  }

  /** Net change in the number of endpoints the effect produces. */
  public int endpointDelta() {
    switch (this) {
      case ADD:
        return 1;
      case REMOVE:
        return -1;
      default:
        return 0;
    }
  }
}

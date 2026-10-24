package com.redis.test.fi;

/**
 * A client-observable outcome the fault injector can produce, named as its API expects it. Each
 * effect family ({@link StandaloneEffect}, {@link SlotMigrateEffect}) has its own discovery route
 * and trigger action; {@link FaultInjectorClient} dispatches on the concrete type.
 */
public interface EffectType {

  /** The effect name as the fault injector API expects it. */
  String wireName();
}

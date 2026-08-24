package com.redis.test.fi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preloaded topology-change-standalone catalog: every {@link StandaloneEffect} resolved to its
 * {@link Effect} in one discovery pass. The catalog is static per FI instance.
 */
public final class StandaloneTriggerCatalog {

  private final Map<StandaloneEffect, Effect> effects;

  private StandaloneTriggerCatalog(Map<StandaloneEffect, Effect> effects) {
    this.effects = effects;
  }

  public static StandaloneTriggerCatalog resolve(FaultInjectorClient client) {
    Map<StandaloneEffect, Effect> effects = new LinkedHashMap<>();
    for (StandaloneEffect type : StandaloneEffect.values()) {
      effects.put(type, client.getStandaloneTriggers(type));
    }
    return new StandaloneTriggerCatalog(effects);
  }

  public Effect effect(StandaloneEffect type) {
    return effects.get(type);
  }

  /** All triggers of all effects, in discovery order. */
  public List<Trigger> allTriggers() {
    List<Trigger> all = new ArrayList<>();
    for (Effect effect : effects.values()) {
      all.addAll(effect.triggers());
    }
    return all;
  }

}

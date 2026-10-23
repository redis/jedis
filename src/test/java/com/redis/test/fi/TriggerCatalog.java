package com.redis.test.fi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preloaded catalog of one effect family: every constant of an {@link EffectType} enum resolved to
 * its {@link Effect} in one discovery pass. The catalog is static per FI instance.
 */
public final class TriggerCatalog {

  private final Map<EffectType, Effect> effects;

  private TriggerCatalog(Map<EffectType, Effect> effects) {
    this.effects = effects;
  }

  public static <E extends Enum<E> & EffectType> TriggerCatalog resolve(FaultInjectorClient client,
      Class<E> family) {
    Map<EffectType, Effect> effects = new LinkedHashMap<>();
    for (E type : family.getEnumConstants()) {
      effects.put(type, client.discover(type));
    }
    return new TriggerCatalog(effects);
  }

  /** The discovered effect; fails loudly for a type outside this catalog's family. */
  public Effect effect(EffectType type) {
    Effect effect = effects.get(type);
    if (effect == null) {
      throw new IllegalArgumentException(type + " is not in this catalog: " + effects.keySet());
    }
    return effect;
  }

  /** All triggers of all effects, in discovery order. */
  public List<Trigger> allTriggers() {
    List<Trigger> all = new ArrayList<>();
    for (Effect effect : effects.values()) {
      all.addAll(effect.triggers());
    }
    return all;
  }

  /**
   * Whether the FI offers the combination on this server: effect, trigger and requirement config.
   */
  public boolean offers(ScenarioKey key) {
    Effect effect = effects.get(key.effect());
    return effect != null && effect.triggers().stream()
        .anyMatch(t -> t.name().equals(key.trigger()) && t.offers(key.config()));
  }

  /** The runnable scenario for the key; fails loudly when the FI does not offer it. */
  public Scenario scenario(ScenarioKey key) {
    return effect(key.effect()).trigger(key.trigger()).scenario(key.config());
  }
}

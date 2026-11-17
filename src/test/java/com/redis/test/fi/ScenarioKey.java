package com.redis.test.fi;

import java.util.Objects;

/**
 * The combination a test matrix asks for: an effect, a trigger name and a requirement config. Pure
 * data, so a matrix can list every row it wants regardless of what the fault injector offers on the
 * current server; {@link TriggerCatalog#offers(ScenarioKey)} answers that, and
 * {@link TriggerCatalog#scenario(ScenarioKey)} resolves the key into a runnable {@link Scenario}.
 */
public final class ScenarioKey {

  private final EffectType effect;
  private final String trigger;
  private final String config;

  private ScenarioKey(EffectType effect, String trigger, String config) {
    this.effect = Objects.requireNonNull(effect, "effect");
    this.trigger = Objects.requireNonNull(trigger, "trigger");
    this.config = Objects.requireNonNull(config, "config");
  }

  public static ScenarioKey of(EffectType effect, String trigger, String config) {
    return new ScenarioKey(effect, trigger, config);
  }

  public EffectType effect() {
    return effect;
  }

  public String trigger() {
    return trigger;
  }

  /** The requirement config name, e.g. {@code external/ip+asm}. */
  public String config() {
    return config;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ScenarioKey)) {
      return false;
    }
    ScenarioKey other = (ScenarioKey) o;
    return effect.equals(other.effect) && trigger.equals(other.trigger)
        && config.equals(other.config);
  }

  @Override
  public int hashCode() {
    return Objects.hash(effect, trigger, config);
  }

  /** Same form as {@link Scenario#toString()}, so a row reads the same whether offered or not. */
  @Override
  public String toString() {
    return effect + "/" + trigger + "[" + config + "]";
  }
}

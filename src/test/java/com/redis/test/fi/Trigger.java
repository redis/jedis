package com.redis.test.fi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** A cluster recipe producing its {@link Effect}, with the requirements it can run under. */
public final class Trigger {

  private final StandaloneEffect effect;
  private final String name;
  private final String description;
  private final List<Requirement> requirements;

  private Trigger(StandaloneEffect effect, String name, String description,
      List<Requirement> requirements) {
    this.effect = effect;
    this.name = name;
    this.description = description;
    this.requirements = Collections.unmodifiableList(requirements);
  }

  @SuppressWarnings("unchecked")
  static Trigger parse(StandaloneEffect effect, Map<String, Object> trigger) {
    List<Requirement> requirements = new ArrayList<>();
    for (Object r : (List<Object>) trigger.get("requirements")) {
      requirements.add(Requirement.parse((Map<String, Object>) r));
    }
    return new Trigger(effect, (String) trigger.get("name"), (String) trigger.get("description"),
        requirements);
  }

  public StandaloneEffect effect() {
    return effect;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  /** A scenario running this trigger under the named requirement config; fails loudly if absent. */
  public Scenario scenario(String config) {
    return new Scenario(this, requirement(config));
  }

  /** Whether the FI environment offers the named requirement config for this trigger. */
  public boolean offers(String config) {
    return requirements.stream().anyMatch(r -> config.equals(r.config()));
  }

  /** The requirement with the given catalog config name; fails loudly if absent. */
  public Requirement requirement(String config) {
    return requirements.stream().filter(r -> config.equals(r.config())).findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Trigger " + name + " has no requirement config '" + config + "': " + requirements));
  }

  public List<Requirement> requirements() {
    return requirements;
  }

  /** The base 'single' variant — requirements[0]; TLS/scaled variants are FI-flag opt-ins. */
  public Requirement requirement() {
    return requirements.get(0);
  }

  /** This trigger under its base requirement. */
  public Scenario scenario() {
    return new Scenario(this, requirement());
  }

  @Override
  public String toString() {
    return effect + "/" + name;
  }
}

package com.redis.test.fi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * One discovered effect: the triggers that produce it, each with the database requirements it can
 * run under. Typed envelope of a discovery response; the dbconfigs inside stay opaque.
 */
public final class Effect {

  private final EffectType type;
  private final int clusterNodes;
  private final List<Trigger> triggers;

  private Effect(EffectType type, int clusterNodes, List<Trigger> triggers) {
    this.type = type;
    this.clusterNodes = clusterNodes;
    this.triggers = Collections.unmodifiableList(triggers);
  }

  @SuppressWarnings("unchecked")
  static Effect parse(EffectType type, Map<String, Object> response) {
    Map<String, Object> cluster = (Map<String, Object>) response.get("cluster");
    int nodes = cluster == null ? 0 : ((Number) cluster.get("nodes")).intValue();

    List<Trigger> triggers = new ArrayList<>();
    for (Object t : (List<Object>) response.get("triggers")) {
      triggers.add(Trigger.parse(type, (Map<String, Object>) t));
    }
    return new Effect(type, nodes, triggers);
  }

  /**
   * The same effect discovered through several routes (e.g. {@code asm=false} and {@code asm=true}
   * of {@code /slot-migrate}, which list disjoint triggers) folded into one trigger list.
   */
  static Effect merge(Effect first, Effect second) {
    List<Trigger> triggers = new ArrayList<>(first.triggers);
    for (Trigger trigger : second.triggers) {
      if (first.triggers.stream().noneMatch(t -> t.name().equals(trigger.name()))) {
        triggers.add(trigger);
      }
    }
    return new Effect(first.type, Math.max(first.clusterNodes, second.clusterNodes), triggers);
  }

  /** This effect without the triggers matching {@code dropped}; the rest keep their order. */
  Effect without(Predicate<Trigger> dropped) {
    List<Trigger> kept = new ArrayList<>();
    for (Trigger trigger : triggers) {
      if (!dropped.test(trigger)) {
        kept.add(trigger);
      }
    }
    return new Effect(type, clusterNodes, kept);
  }

  public EffectType type() {
    return type;
  }

  public int clusterNodes() {
    return clusterNodes;
  }

  public List<Trigger> triggers() {
    return triggers;
  }

  /** The named trigger; fails loudly when the FI does not offer it for this effect. */
  public Trigger trigger(String name) {
    for (Trigger trigger : triggers) {
      if (trigger.name().equals(name)) {
        return trigger;
      }
    }
    throw new IllegalArgumentException(
        "FI offers no trigger '" + name + "' for " + type + "; available: " + triggers);
  }
}

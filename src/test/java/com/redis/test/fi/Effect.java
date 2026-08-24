package com.redis.test.fi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A client-observable outcome of {@code topology_change_standalone} with the triggers that produce
 * it — the typed, read-only envelope of one discovery response. Dbconfigs inside stay opaque maps;
 * their schema belongs to the FI / Redis Enterprise.
 */
public final class Effect {

  private final StandaloneEffect type;
  private final int clusterNodes;
  private final List<Trigger> triggers;

  private Effect(StandaloneEffect type, int clusterNodes, List<Trigger> triggers) {
    this.type = type;
    this.clusterNodes = clusterNodes;
    this.triggers = Collections.unmodifiableList(triggers);
  }

  @SuppressWarnings("unchecked")
  static Effect parse(StandaloneEffect type, Map<String, Object> response) {
    Map<String, Object> cluster = (Map<String, Object>) response.get("cluster");
    int nodes = cluster == null ? 0 : ((Number) cluster.get("nodes")).intValue();

    List<Trigger> triggers = new ArrayList<>();
    for (Object t : (List<Object>) response.get("triggers")) {
      triggers.add(Trigger.parse(type, (Map<String, Object>) t));
    }
    return new Effect(type, nodes, triggers);
  }

  public StandaloneEffect type() {
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

package com.redis.test.fi;

/**
 * One runnable test combination: a {@link Trigger} under one of its {@link Requirement}s. The
 * effect is derived from the trigger, never stored — a scenario cannot be inconsistent.
 */
public final class Scenario {

  private final Trigger trigger;
  private final Requirement requirement;

  Scenario(Trigger trigger, Requirement requirement) {
    this.trigger = trigger;
    this.requirement = requirement;
  }

  public StandaloneEffect effect() {
    return trigger.effect();
  }

  public Trigger trigger() {
    return trigger;
  }

  public Requirement requirement() {
    return requirement;
  }

  @Override
  public String toString() {
    return trigger + "[" + requirement.config() + "]";
  }
}

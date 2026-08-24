package com.redis.test.fi;

/**
 * Client-observable outcomes of the fault injector's {@code topology_change_standalone} action.
 * Each effect is produced by one or more named triggers; see
 * {@link FaultInjectorClient#getStandaloneTriggers(StandaloneEffect)}.
 */
public enum StandaloneEffect {

  /** Data moves and the endpoint rebinds (connection drop). */
  DATA_MOVEMENT_CONN_DROP("data_movement_conn_drop"),
  /** Data moves while the endpoint stays (hitless). */
  DATA_MOVEMENT_NO_CONN_DROP("data_movement_no_conn_drop"),
  /** The endpoint rebinds without moving data. */
  CONN_DROP("conn_drop"),
  /** The endpoint policy changes, altering what its DNS name resolves to. */
  DNS_RESOLUTION_CHANGE("dns_resolution_change");

  private final String wireName;

  StandaloneEffect(String wireName) {
    this.wireName = wireName;
  }

  /** The effect name as the fault injector API expects it. */
  public String wireName() {
    return wireName;
  }
}

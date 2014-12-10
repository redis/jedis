package redis.clients.util;

import redis.clients.jedis.HostAndPort;

import java.util.ArrayList;
import java.util.List;

public class ClusterNodeInformation {
  private HostAndPort node;
  private List<Integer> availableSlots;
  private List<Integer> slotsBeingImported;
  private List<Integer> slotsBeingMigrated;

  public ClusterNodeInformation(HostAndPort node) {
    this.node = node;
    this.availableSlots = new ArrayList<Integer>();
    this.slotsBeingImported = new ArrayList<Integer>();
    this.slotsBeingMigrated = new ArrayList<Integer>();
  }

  public void addAvailableSlot(int slot) {
    availableSlots.add(slot);
  }

  public void addSlotBeingImported(int slot) {
    slotsBeingImported.add(slot);
  }

  public void addSlotBeingMigrated(int slot) {
    slotsBeingMigrated.add(slot);
  }

  public HostAndPort getNode() {
    return node;
  }

  public List<Integer> getAvailableSlots() {
    return availableSlots;
  }

  public List<Integer> getSlotsBeingImported() {
    return slotsBeingImported;
  }

  public List<Integer> getSlotsBeingMigrated() {
    return slotsBeingMigrated;
  }
}

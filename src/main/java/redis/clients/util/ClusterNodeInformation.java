package redis.clients.util;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.HostAndPort;

public class ClusterNodeInformation {
  private HostAndPort node;
  private List<Integer> availableSlots;

  public ClusterNodeInformation(HostAndPort node) {
    this.node = node;
    this.availableSlots = new ArrayList<Integer>();
  }

  public void addAvailableSlot(int slot) {
    availableSlots.add(slot);
  }

  public HostAndPort getNode() {
    return node;
  }

  public List<Integer> getAvailableSlots() {
    return availableSlots;
  }

}

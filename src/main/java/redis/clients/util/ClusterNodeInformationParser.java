package redis.clients.util;

import redis.clients.jedis.HostAndPort;

import java.util.List;

public class ClusterNodeInformationParser {
  
  public ClusterNodeInformation parse(List<Object> nodeInfo) {

    Long slotFrom = (Long) nodeInfo.get(0);
    Long slotTo = (Long) nodeInfo.get(1);
    List<Object> nodeMasterData = (List<Object>) nodeInfo.get(2);
    String masterHost = new String((byte[]) nodeMasterData.get(0));
    Long masterPort = (Long) nodeMasterData.get(1);

    HostAndPort node = new HostAndPort(masterHost, masterPort.intValue());

    ClusterNodeInformation info = new ClusterNodeInformation(node);

    fillSlotInformation(slotFrom, slotTo, info);

    return info;
  }

  private void fillSlotInformation(Long slotFrom, Long slotTo , ClusterNodeInformation info) {
    for (int slot = slotFrom.intValue(); slot <= slotTo.intValue(); slot++) {
      info.addAvailableSlot(slot);
    }
  }
}

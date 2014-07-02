package redis.clients.jedis;

import redis.clients.util.ClusterNodeInformation;
import redis.clients.util.ClusterNodeInformationParser;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JedisClusterInfoCache {
    public static final ClusterNodeInformationParser nodeInfoParser = new ClusterNodeInformationParser();

    private Map<String, JedisPool> nodes = new HashMap<String, JedisPool>();
    private Map<Integer, JedisPool> slots = new HashMap<Integer, JedisPool>();

    public synchronized void discoverClusterNodesAndSlots(Jedis jedis) {
        this.nodes.clear();
        this.slots.clear();

        String localNodes = jedis.clusterNodes();
        for (String nodeInfo : localNodes.split("\n")) {
            ClusterNodeInformation clusterNodeInfo = nodeInfoParser.parse(
                    nodeInfo, new HostAndPort(jedis.getClient().getHost(),
                            jedis.getClient().getPort()));

            HostAndPort targetNode = clusterNodeInfo.getNode();
            setNodeIfNotExist(targetNode);
            assignSlotsToNode(clusterNodeInfo.getAvailableSlots(), targetNode);
        }
    }

    public synchronized void discoverClusterSlots(Jedis jedis) {
        this.slots.clear();

        List<Object> slots = jedis.clusterSlots();

        for (Object slotInfoObj : slots) {
            List<Object> slotInfo = (List<Object>) slotInfoObj;

            if (slotInfo.size() <= 2) {
                continue;
            }

            // assigned slots
            List<Integer> slotNums = new ArrayList<Integer>();
            for (int slot = ((Long) slotInfo.get(0)).intValue() ;
                 slot <= ((Long) slotInfo.get(1)).intValue() ;
                 slot++) {
                slotNums.add(slot);
            }

            // hostInfos
            List<Object> hostInfos = (List<Object>) slotInfo.get(2);
            if (hostInfos.size() <= 0) {
                continue;
            }

            // at this time, we just use master, discard slave information
            HostAndPort targetNode = new HostAndPort(
                    SafeEncoder.encode((byte[]) hostInfos.get(0)),
                    ((Long) hostInfos.get(1)).intValue());

            setNodeIfNotExist(targetNode);
            assignSlotsToNode(slotNums, targetNode);
        }
    }

    public synchronized void setNodeIfNotExist(HostAndPort node) {
        String nodeKey = getNodeKey(node);
        if (nodes.containsKey(nodeKey))
            return;

        JedisPool nodePool = new JedisPool(node.getHost(), node.getPort());
        nodes.put(nodeKey, nodePool);
    }

    public synchronized void assignSlotToNode(int slot, HostAndPort targetNode) {
        JedisPool targetPool = nodes.get(getNodeKey(targetNode));

        if (targetPool == null) {
            setNodeIfNotExist(targetNode);
            targetPool = nodes.get(getNodeKey(targetNode));
        }
        slots.put(slot, targetPool);
    }

    public synchronized void assignSlotsToNode(List<Integer> targetSlots,
                                  HostAndPort targetNode) {
        JedisPool targetPool = nodes.get(getNodeKey(targetNode));

        if (targetPool == null) {
            setNodeIfNotExist(targetNode);
            targetPool = nodes.get(getNodeKey(targetNode));
        }

        for (Integer slot : targetSlots) {
            slots.put(slot, targetPool);
        }
    }

    public synchronized JedisPool getNode(String nodeKey) {
        return nodes.get(nodeKey);
    }

    public synchronized JedisPool getSlotPool(int slot) {
        return slots.get(slot);
    }

    public synchronized Map<String, JedisPool> getNodes() {
        return new HashMap<String, JedisPool>(nodes);
    }

    public static String getNodeKey(HostAndPort hnp) {
        return hnp.getHost() + ":" + hnp.getPort();
    }

    public static String getNodeKey(Client client) {
        return client.getHost() + ":" + client.getPort();
    }

    public static String getNodeKey(Jedis jedis) {
        return getNodeKey(jedis.getClient());
    }

}

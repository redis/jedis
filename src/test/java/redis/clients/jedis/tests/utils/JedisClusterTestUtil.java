package redis.clients.jedis.tests.utils;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

public class JedisClusterTestUtil {
  public static void waitForClusterReady(Jedis... nodes) throws InterruptedException {
    boolean clusterOk = false;
    while (!clusterOk) {
      boolean isOk = true;
      for (Jedis node : nodes) {
        if (!node.clusterInfo().split("\n")[0].contains("ok")) {
          isOk = false;
          break;
        }
      }

      if (isOk) {
        clusterOk = true;
      }

      Thread.sleep(50);
    }
  }

  public static String getNodeId(String infoOutput) {
    for (String infoLine : infoOutput.split("\n")) {
      if (infoLine.contains("myself")) {
        return infoLine.split(" ")[0];
      }
    }
    return "";
  }

  public static String getNodeId(String infoOutput, HostAndPort node) {

    for (String infoLine : infoOutput.split("\n")) {
      if (infoLine.contains(node.toString())) {
        return infoLine.split(" ")[0];
      }
    }
    return "";
  }

  public static void assertNodeIsKnown(Jedis node, String targetNodeId, int timeoutMs) {
    assertNodeRecognizedStatus(node, targetNodeId, true, timeoutMs);
  }

  public static void assertNodeIsUnknown(Jedis node, String targetNodeId, int timeoutMs) {
    assertNodeRecognizedStatus(node, targetNodeId, false, timeoutMs);
  }

  private static void assertNodeRecognizedStatus(Jedis node, String targetNodeId,
      boolean shouldRecognized, int timeoutMs) {
    int sleepInterval = 100;
    for (int sleepTime = 0; sleepTime <= timeoutMs; sleepTime += sleepInterval) {
      boolean known = isKnownNode(node, targetNodeId);
      if (shouldRecognized == known) return;

      try {
        Thread.sleep(sleepInterval);
      } catch (InterruptedException e) {
      }
    }

    throw new JedisException("Node recognize check error");
  }

  private static boolean isKnownNode(Jedis node, String nodeId) {
    String infoOutput = node.clusterNodes();
    for (String infoLine : infoOutput.split("\n")) {
      if (infoLine.contains(nodeId)) {
        return true;
      }
    }
    return false;
  }

}

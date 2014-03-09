package redis.clients.jedis.tests.utils;

import redis.clients.jedis.Jedis;

public class JedisClusterTestUtil {
    public static void waitForClusterReady(Jedis...nodes) throws InterruptedException {
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
    
}

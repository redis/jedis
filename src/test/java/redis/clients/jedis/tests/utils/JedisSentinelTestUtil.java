package redis.clients.jedis.tests.utils;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

public class JedisSentinelTestUtil {

    public static void waitForSentinelRecognizeRedisReplication(
	    HostAndPort sentinel, String masterName, HostAndPort master,
	    List<HostAndPort> slaves) throws InterruptedException {
	Jedis sentinelJedis = new Jedis(sentinel.getHost(), sentinel.getPort());
	while (true) {
	    Thread.sleep(1000);

	    if (!isMasterRecognized(sentinelJedis, masterName, master)) {
		System.out.println("Master not recognized by Sentinel "
			+ sentinel.getHost() + ":" + sentinel.getPort()
			+ ", sleep...");
		continue;
	    }

	    if (!isSlavesRecognized(sentinelJedis, masterName, slaves)) {
		System.out.println("Slaves not recognized by Sentinel "
			+ sentinel.getHost() + ":" + sentinel.getPort()
			+ ", sleep...");
		continue;
	    }

	    // all recognized
	    break;
	}

    }

    public static HostAndPort waitForNewPromotedMaster(HostAndPort sentinel,
	    String masterName, HostAndPort oldMaster)
	    throws InterruptedException {
	Jedis sentinelJedis = new Jedis(sentinel.getHost(), sentinel.getPort());

	HostAndPort newMaster = null;
	while (true) {
	    Thread.sleep(1000);

	    List<String> sentinelMasterInfos = sentinelJedis
		    .sentinelGetMasterAddrByName(masterName);
	    if (sentinelMasterInfos == null)
		continue;

	    newMaster = new HostAndPort(sentinelMasterInfos.get(0),
		    Integer.parseInt(sentinelMasterInfos.get(1)));

	    if (!newMaster.equals(oldMaster))
		break;

	    System.out
		    .println("Sentinel's master is not yet changed, sleep...");
	}

	return newMaster;
    }

    public static void waitForSentinelsRecognizeEachOthers()
	    throws InterruptedException {
	// During failover, master has been changed
	// It means that sentinels need to recognize other sentinels from new
	// master's hello channel
	// Without recognizing, Sentinels cannot run failover

	// Sentinels need to take some time to recognize each other...
	// http://redis.io/topics/sentinel
	// Sentinel Rule #8: Every Sentinel publishes a message to every
	// monitored master
	// Pub/Sub channel __sentinel__:hello, every five seconds, blabla...

	// FIXME There're no command for sentinel to list recognized sentinels
	// so sleep wisely (channel's hello message interval + margin)
	Thread.sleep(5000 + 500);
    }

    private static boolean isMasterRecognized(Jedis sentinelJedis,
	    String masterName, HostAndPort master) {
	List<String> sentinelMasterInfos = sentinelJedis
		.sentinelGetMasterAddrByName(masterName);
	if (sentinelMasterInfos == null)
	    return false;

	HostAndPort sentinelMaster = new HostAndPort(
		sentinelMasterInfos.get(0),
		Integer.parseInt(sentinelMasterInfos.get(1)));

	return sentinelMaster.equals(master);
    }

    private static boolean isSlavesRecognized(Jedis sentinelJedis,
	    String masterName, List<HostAndPort> slaves) {
	List<Map<String, String>> slavesMap = sentinelJedis
		.sentinelSlaves(masterName);

	if (slavesMap.size() != slaves.size())
	    return false;

	int slavesRecognized = 0;

	for (HostAndPort slave : slaves) {
	    if (isSlaveFoundInSlavesMap(slavesMap, slave))
		slavesRecognized++;
	}

	return slavesRecognized == slaves.size();
    }

    private static boolean isSlaveFoundInSlavesMap(
	    List<Map<String, String>> slavesMap, HostAndPort slave) {
	for (Map<String, String> slaveMap : slavesMap) {
	    HostAndPort sentinelSlave = new HostAndPort(slaveMap.get("ip"),
		    Integer.parseInt(slaveMap.get("port")));

	    if (sentinelSlave.equals(slave))
		return true;
	}

	return false;
    }

}

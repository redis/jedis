package redis.clients.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.BasicCommands;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.JedisServerInfo;
import redis.clients.jedis.exceptions.JedisException;

public class FailSafeJedisCluster {

	private Collection<JedisServerInfo> members = new ArrayList<JedisServerInfo>();
	private Map<JedisServerInfo, BasicCommands> monitors = new HashMap<JedisServerInfo, BasicCommands>();
	private JedisServerInfo elected = null;

	public FailSafeJedisCluster(JedisServerInfo... vinfs) {
		for (JedisServerInfo info : vinfs) {
			this.members.add(info);
			BinaryJedis jedis = new BinaryJedis(info.getHost(), info.getPort());
			if (info.getPassword() != null)
				jedis.auth(info.getPassword());
			this.monitors.put(info, jedis);
		}
		electNewMaster();
	}

	public JedisServerInfo getMaster() {
		return elected;
	}

	public Collection<JedisServerInfo> getMembers() {
		return Collections.unmodifiableCollection(members);
	}
	
	public BasicCommands getBasicCommands(JedisServerInfo info) {
		return monitors.get(info);
	}

	public synchronized JedisServerInfo electNewMaster() {
		if (this.elected == null || !eligible(this.elected)) {
			this.elected = null;

			for (JedisServerInfo m : this.members) {
				if (eligible(m)) {
					this.elected = m;
					return this.elected;
				}
			}
			throw new JedisException("No master is found");
		}
		return this.elected;
	}
	
	public synchronized void resetMaster(JedisServerInfo newMaster){
		if (monitors.get(newMaster) == null)
			throw new JedisException(newMaster.toString() + " is not member of the cluster");
		
		for (JedisServerInfo m : this.members) {
			BasicCommands jedis = monitors.get(m);
			if (m == newMaster) {
				jedis.slaveofNoOne();
			} else {
				jedis.slaveof(newMaster.getHost(), newMaster.getPort());
			}
		}
		this.elected = newMaster;
		
	}

	private boolean eligible(JedisServerInfo info) {
		BasicCommands jedis = monitors.get(info);
		try {
			for (String line : jedis.info().split("\r\n")) {
				String[] pair = line.split(":");
				if (pair.length > 1 && pair[0].equals("role")
						&& pair[1].equals("master"))
					return true;
			}
		} catch (JedisException e) {
		}
		return false;
	}

}

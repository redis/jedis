package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.util.Pair;
import redis.clients.util.Pool;

public class JedisSentinelSlavePool extends JedisSentinelPool {

    /**
     * Map of slave and its Jedis pool
     */
    protected final Map<HostAndPort, Pool<Jedis>> slavePool;
    private volatile AtomicInteger index ;

    public JedisSentinelSlavePool(final String masterName,
            final Set<String> sentinels,
            final GenericObjectPoolConfig poolConfig) {
        this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, 
                null,
                Protocol.DEFAULT_DATABASE);
    }

    public JedisSentinelSlavePool(final String masterName,
            final Set<String> sentinels,
            final GenericObjectPoolConfig poolConfig, int timeout,
            final String password, final int database) {
        super(masterName, sentinels, poolConfig, timeout, password,
                database);
        this.slavePool
                = Collections.synchronizedMap(
                        new LinkedHashMap<HostAndPort, Pool<Jedis>>());
        this.index = new AtomicInteger(-1);
        this.initSentinels(sentinels, masterName);
    }

    private void initSentinels(final Set<String> sentinels,
            final String masterName) {
        boolean sentinelAvailable = false;
        log.log(Level.INFO, "Trying to find the slaves for master : {0}", 
                masterName);
        // Connect to each sentinel and register pub sub
        for (final String sentinel : sentinels) {
            final HostAndPort hap = toHostAndPort(Arrays.asList(sentinel
                    .split(":")));
            // Connect to the Sentinel and retrieve Slave information
            Jedis jedis = null;
            try {
                log.log(Level.FINE, "Connecting to Sentinel {0}", hap);

                jedis = new Jedis(hap.getHost(), hap.getPort());

                final List<Map<String, String>> slaves = jedis.
                        sentinelSlaves(masterName);
                
                sentinelAvailable = true;
                
                if (slaves.size() > 0) {
                    log.log(Level.INFO, "Found {0} slaves from sentinel: {1}",
                            new Object[]{slaves.size(), hap});
                    initSlavePool(slaves);
                }
                break;
            } catch (JedisConnectionException jedisConnectionException) {
                log.log(Level.WARNING,
                        "Cannot connect to "
                        + "sentinel running @ {0}. Trying next one.",
                        hap);
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }
        if (!sentinelAvailable){
		throw new JedisConnectionException(
			"All sentinels down, cannot determine the slaves for"
				+ masterName + " master");            
        }
        initSentinelListeners(sentinels, masterName, 
                new Pair<JedisSentinelPubSubAdapter, String[]>(
                        new JedisSentinelPubSubAdapter() {

                    @Override
                    public boolean isValidMessage(final String channel,
                            final String[] messageParts) {
                        return null != messageParts && messageParts.length > 6;
                    }

                    @Override
                    public String getMasterName(final String[] messageParts) {
                        return null != messageParts && messageParts.length > 6
                        ? messageParts[5] : null;
                    }

                    @Override
                    public void handleMessage(final String channel,
                            final String[] messageParts) {
                        final HostAndPort host = new HostAndPort(
                                messageParts[2],
                                Integer.parseInt(messageParts[3]));
                        if ("+slave".equals(channel) ||
                                "-sdown".equals(channel)) {
                            addSlave(host);
                        } else if ("+sdown".equals(channel)) {
                            removeSlave(host);
                        }
                    }
                }, new String[]{"+slave", "+sdown", "-sdown"}));
    }

    protected void addSlave(final HostAndPort hostAndPort) {
        if (null != hostAndPort) {
            synchronized(this.slavePool){
                if (!this.slavePool.containsKey(hostAndPort)) {
                    final Pool<Jedis> pool = new JedisPool(poolConfig, 
                            hostAndPort.getHost(), hostAndPort.getPort(),timeout,
                            password,database);
                    this.slavePool.put(hostAndPort,pool);
                }
            }
        }
    }

    protected void removeSlave(final HostAndPort hostAndPort) {
        Pool<Jedis> slave ;
        synchronized (this.slavePool) {
            slave = this.slavePool.remove(hostAndPort);
        }
        if (slave == null) {
            log.log(Level.WARNING,
                    "Slave was not part of the internal Pool{0}",
                    hostAndPort);
            return;
        }
        slave.close();
    }

    private void initSlavePool(final Collection<Map<String, String>> slaves) {
        for (final Map<String, String> slave : slaves) {
            final String hap = slave.get("name");
            if (null != hap) {
                addSlave(toHostAndPort(Arrays.asList(hap.split(":"))));
            }
        }
    }

    /**
     * Returns the Jedis instance of the slave if it exists
     * @param hostName 
     * @param port
     * @return
     * @throws JedisConnectionException 
     */
    public Jedis getSlave(final String hostName, final int port) 
                throws JedisConnectionException{
        final Pool<Jedis> slave = this.slavePool.
                get(new HostAndPort(hostName, port));
        return null != slave ? slave.getResource() : null;
    }
    
    /**
     * @return A slave if it exists, if not, will return the master
     */
    public Jedis getSlave(){
        final HostAndPort slave = pickSlave(getSlaves());
        return slave == null ? this.getResource() : 
                getSlave(slave.getHost(),slave.getPort());
    }

    /**
     * @return List of slaves for the master
     */
    public Collection<HostAndPort> getSlaves() {
        synchronized (this.slavePool){
            return Collections.unmodifiableCollection(this.slavePool.keySet());
        }
    }

    public Map<HostAndPort, Pool<Jedis>> getSlaveConfig(){
        synchronized (this.slavePool){
            return Collections.unmodifiableMap(this.slavePool);
        }
    }
    
    @Override
    public void destroy() {
        synchronized (this.slavePool){
            for (final Pool<Jedis> slave : slavePool.values()) {
                slave.close();
            }
            this.slavePool.clear();
        }
        super.destroy();
    }

    /**
     * @param slaves
     * @return 
     */
    protected HostAndPort pickSlave(final Collection<HostAndPort> slaves) {
        if (slaves.isEmpty()){
            log.warning("No available slave to pick from");
            return null;
        }
        final List<HostAndPort> hostAndPorts = new ArrayList<HostAndPort>(slaves);
        final int localIndex = Math.abs(index.incrementAndGet() % hostAndPorts.size());
        return hostAndPorts.get(localIndex);
    }
}

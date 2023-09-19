package redis.clients.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * active detect master node .in case of the subscribe message  lost
 * @see  SentinelMasterSubscribeListener  subscribe failover message from "+switch-master" channel
 *
 */
public abstract class SentinelMasterActiveDetectListener extends Thread implements SentinelMasterListener {

    private static final Logger LOG = LoggerFactory.getLogger(SentinelMasterActiveDetectListener.class);

    private List<String> currentHostMaster;
    private HostAndPort sentinel;
    private JedisClientConfig jedisClientConfig;
    private String masterName;
    private long activeDetectIntervalTimeMillis = 5 * 1000;

    private AtomicBoolean running = new AtomicBoolean(false);
    private volatile Jedis j;

    public SentinelMasterActiveDetectListener(HostAndPort currentHostMaster, HostAndPort sentinel,
                                              JedisClientConfig jedisClientConfig, String masterName,
                                              long activeDetectIntervalTimeMillis) {
        super(String.format("SentinelMasterActiveDetectListener-%s-[%s:%d]", masterName, sentinel.getHost(), sentinel.getPort()));
        this.currentHostMaster = Arrays.asList(currentHostMaster.getHost(), String.valueOf(currentHostMaster.getPort()));
        this.sentinel = sentinel;
        this.jedisClientConfig = jedisClientConfig;
        this.masterName = masterName;
        this.activeDetectIntervalTimeMillis = activeDetectIntervalTimeMillis;
    }

    @Override
    public void shutdown() {
        LOG.info("Shutting down active detect listener on {}", sentinel);
        running.set(false);
        if (j != null) {
            j.close();
        }
    }

    @Override
    public void run() {
        LOG.info("Start active detect listener on {},interval {} ms", sentinel, activeDetectIntervalTimeMillis);
        running.set(true);
        j = new Jedis(sentinel, jedisClientConfig);
        while (running.get()) {
            try {
                Thread.sleep(activeDetectIntervalTimeMillis);

                if (j == null || j.isBroken() || !j.isConnected()) {
                    j = new Jedis(sentinel, jedisClientConfig);
                }

                List<String> masterAddr = j.sentinelGetMasterAddrByName(masterName);
                if (masterAddr == null || masterAddr.size() != 2) {
                    LOG.warn("Can not get master addr, master name: {}. Sentinel: {}", masterName, sentinel);
                    continue;
                }

                if (!currentHostMaster.equals(masterAddr)) {
                    LOG.info("Found master node change from {} to{} ", currentHostMaster, masterAddr);
                    onChange(new HostAndPort(masterAddr.get(0), Integer.parseInt(masterAddr.get(1))));
                    this.currentHostMaster = masterAddr;
                }
            } catch (Exception e) {
                // TO  ensure the thread running, catch all exception
                LOG.error("Active detect listener failed ", e);
            }
        }
    }

    public abstract void onChange(HostAndPort hostAndPort);
}

package redis.clients.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * subscribe failover message from "+switch-master" channel , the default listener mode use this
 * @see  SentinelMasterActiveDetectListener  active detect master node .in case of the subscribe message  lost
 *
 */
public abstract class SentinelMasterSubscribeListener extends Thread implements SentinelMasterListener {

    private static final Logger LOG = LoggerFactory.getLogger(SentinelMasterSubscribeListener.class);

    private String masterName;
    private HostAndPort sentinel;
    private JedisClientConfig sentinelClientConfig;
    private long subscribeRetryWaitTimeMillis = 5000;
    private volatile Jedis j;
    private AtomicBoolean running = new AtomicBoolean(false);


    public SentinelMasterSubscribeListener(String masterName, HostAndPort sentinel, JedisClientConfig sentinelClientConfig,
                                          long subscribeRetryWaitTimeMillis) {
        super(String.format("SentinelMaterSubscribeListener-%s-[%s:%d]", masterName, sentinel.getHost(), sentinel.getPort()));
        this.masterName = masterName;
        this.sentinel = sentinel;
        this.sentinelClientConfig = sentinelClientConfig;
        this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
    }

    @Override
    public void run() {

        LOG.info("start on:{}", sentinel);

        running.set(true);

        while (running.get()) {

            try {
                // double check that it is not being shutdown
                if (!running.get()) {
                    break;
                }

                j = new Jedis(sentinel, sentinelClientConfig);

                // code for active refresh
                List<String> masterAddr = j.sentinelGetMasterAddrByName(masterName);
                if (masterAddr == null || masterAddr.size() != 2) {
                    LOG.warn("Can not get master addr, master name: {}. Sentinel: {}.", masterName,
                            sentinel);
                } else {
                    onChange(toHostAndPort(masterAddr));
                }

                j.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        LOG.debug("Sentinel {} published: {}.", sentinel, message);

                        String[] switchMasterMsg = message.split(" ");

                        if (switchMasterMsg.length > 3) {

                            if (masterName.equals(switchMasterMsg[0])) {
                                LOG.info("Receive switch-master message:{} from {}.", message, channel);
                                onChange(toHostAndPort(Arrays.asList(switchMasterMsg[3], switchMasterMsg[4])));
                            } else {
                                LOG.debug(
                                        "Ignoring message on +switch-master for master name {}, our master name is {}",
                                        switchMasterMsg[0], masterName);
                            }

                        } else {
                            LOG.error("Invalid message received on Sentinel {} on channel +switch-master: {}",
                                    sentinel, message);
                        }
                    }
                }, "+switch-master");

            } catch (JedisException e) {

                if (running.get()) {
                    LOG.error("Lost connection to Sentinel at {}. Sleeping {}ms and retrying.", sentinel, subscribeRetryWaitTimeMillis, e);
                    try {
                        Thread.sleep(subscribeRetryWaitTimeMillis);
                    } catch (InterruptedException e1) {
                        LOG.error("Sleep interrupted: ", e1);
                    }
                } else {
                    LOG.debug("Unsubscribing from Sentinel at {}", sentinel);
                }
            } finally {
                if (j != null) {
                    j.close();
                }
            }
        }
    }

    @Override
    public void shutdown() {
        try {
            LOG.debug("Shutting down subscribe listener on {}", sentinel);
            running.set(false);
            // This isn't good, the Jedis object is not thread safe
            if (j != null) {
                j.close();
            }
        } catch (RuntimeException e) {
            LOG.error("Caught exception while shutting down: ", e);
        }
    }


    @Override
    public abstract void onChange(HostAndPort hostAndPort);

    private HostAndPort toHostAndPort(List<String> getMasterAddrByNameResult) {
        String host = getMasterAddrByNameResult.get(0);
        int port = Integer.parseInt(getMasterAddrByNameResult.get(1));

        return new HostAndPort(host, port);
    }
}

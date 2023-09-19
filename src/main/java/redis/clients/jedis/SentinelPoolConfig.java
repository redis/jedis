package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class SentinelPoolConfig extends GenericObjectPoolConfig {

    private boolean enableActiveDetectListener = false;
    private long activeDetectIntervalTimeMillis = 5 * 1000;

    private boolean enableDefaultSubscribeListener = true;
    private long subscribeRetryWaitTimeMillis = 5 * 1000;

    public boolean isEnableActiveDetectListener() {
        return enableActiveDetectListener;
    }

    public void setEnableActiveDetectListener(boolean enableActiveDetectListener) {
        this.enableActiveDetectListener = enableActiveDetectListener;
    }

    public long getActiveDetectIntervalTimeMillis() {
        return activeDetectIntervalTimeMillis;
    }

    public void setActiveDetectIntervalTimeMillis(long activeDetectIntervalTimeMillis) {
        this.activeDetectIntervalTimeMillis = activeDetectIntervalTimeMillis;
    }

    public boolean isEnableDefaultSubscribeListener() {
        return enableDefaultSubscribeListener;
    }

    public void setEnableDefaultSubscribeListener(boolean enableDefaultSubscribeListener) {
        this.enableDefaultSubscribeListener = enableDefaultSubscribeListener;
    }

    public long getSubscribeRetryWaitTimeMillis() {
        return subscribeRetryWaitTimeMillis;
    }

    public void setSubscribeRetryWaitTimeMillis(long subscribeRetryWaitTimeMillis) {
        this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
    }
}

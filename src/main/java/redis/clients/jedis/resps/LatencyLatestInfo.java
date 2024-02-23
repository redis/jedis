package redis.clients.jedis.resps;

import redis.clients.jedis.Builder;

import java.util.List;

import static redis.clients.jedis.BuilderFactory.LONG;
import static redis.clients.jedis.BuilderFactory.STRING;

public class LatencyLatestInfo {

    private final String command;
    private final long timestamp;
    private final long lastEventLatency;
    private final long maxEventLatency;

    public LatencyLatestInfo(String command, long timestamp, long lastEventLatency, long maxEventLatency) {
        this.command = command;
        this.timestamp = timestamp;
        this.lastEventLatency = lastEventLatency;
        this.maxEventLatency = maxEventLatency;
    }

    public String getCommand() {
        return command;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getLastEventLatency() {
        return lastEventLatency;
    }

    public long getMaxEventLatency() {
        return maxEventLatency;
    }

    public static final Builder<LatencyLatestInfo> LATENCY_LATEST_BUILDER = new Builder<LatencyLatestInfo>() {
        @Override
        public LatencyLatestInfo build(Object data) {
            List<Object> commandData = (List<Object>) data;

            String command = STRING.build(commandData.get(0));
            long timestamp = LONG.build(commandData.get(1));
            long lastEventLatency = LONG.build(commandData.get(2));
            long maxEventLatency = LONG.build(commandData.get(3));

            return new LatencyLatestInfo(command, timestamp, lastEventLatency, maxEventLatency);
        }
    };
}

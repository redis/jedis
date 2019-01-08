package redis.clients.jedis.params.stream;

public class PendingInfo {

    private String entryId;

    private String consumer;

    private long idle;

    private long deliveredTimes;

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public long getIdle() {
        return idle;
    }

    public void setIdle(long idle) {
        this.idle = idle;
    }

    public long getDeliveredTimes() {
        return deliveredTimes;
    }

    public void setDeliveredTimes(long deliveredTimes) {
        this.deliveredTimes = deliveredTimes;
    }
}

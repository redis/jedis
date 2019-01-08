package redis.clients.jedis.params.stream;

public class GroupInfo {

    private String name;

    private long consumers;

    private long pending;

    private String lastDeliveredId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getConsumers() {
        return consumers;
    }

    public void setConsumers(long consumers) {
        this.consumers = consumers;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public String getLastDeliveredId() {
        return lastDeliveredId;
    }

    public void setLastDeliveredId(String lastDeliveredId) {
        this.lastDeliveredId = lastDeliveredId;
    }
}

package redis.clients.jedis.params.stream;

import java.util.ArrayList;
import java.util.List;

public class GroupPendingInfo {

    private long count;

    private String oldestEntryId;

    private String newestEntryId;

    private List<ConsumerInfo> consumers = null;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getOldestEntryId() {
        return oldestEntryId;
    }

    public void setOldestEntryId(String oldestEntryId) {
        this.oldestEntryId = oldestEntryId;
    }

    public String getNewestEntryId() {
        return newestEntryId;
    }

    public void setNewestEntryId(String newestEntryId) {
        this.newestEntryId = newestEntryId;
    }

    public List<ConsumerInfo> getConsumers() {
        return consumers == null ? new ArrayList<ConsumerInfo>() : consumers;
    }

    public void setConsumers(List<ConsumerInfo> consumers) {
        this.consumers = consumers;
    }
}

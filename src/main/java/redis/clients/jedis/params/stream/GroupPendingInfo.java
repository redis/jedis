package redis.clients.jedis.params.stream;

import java.util.ArrayList;
import java.util.List;

/**
 * ConsumerGroup的未确认信息统计
 * Created by WangXiao on 11/11/18.
 */
public class GroupPendingInfo {

    /**
     * 未确认信息个数
     */
    private long count;

    /**
     * 最早未确认信息的ID
     */
    private String oldestEntryId;

    /**
     * 最新未确认信息的ID
     */
    private String newestEntryId;

    /**
     * 消费者信息列表
     */
    private List<ConsumerInfo> consumers = null;

    /**
     * 取未确认信息个数
     *
     * @return 未确认信息个数
     */
    public long getCount() {
        return count;
    }

    /**
     * 设置未确认信息个数
     *
     * @param count 未确认信息个数
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * 取最早未确认信息的ID
     *
     * @return 最早未确认信息的ID
     */
    public String getOldestEntryId() {
        return oldestEntryId;
    }

    /**
     * 设置最早未确认信息的ID
     *
     * @param oldestEntryId 最早未确认信息的ID
     */
    public void setOldestEntryId(String oldestEntryId) {
        this.oldestEntryId = oldestEntryId;
    }

    /**
     * 取最新未确认信息的ID
     *
     * @return 最新未确认信息的ID
     */
    public String getNewestEntryId() {
        return newestEntryId;
    }

    /**
     * 设置最新未确认信息的ID
     *
     * @param newestEntryId 最新未确认信息的ID
     */
    public void setNewestEntryId(String newestEntryId) {
        this.newestEntryId = newestEntryId;
    }

    /**
     * 取消费者信息列表
     *
     * @return 消费者信息列表
     */
    public List<ConsumerInfo> getConsumers() {
        return consumers == null ? new ArrayList<ConsumerInfo>() : consumers;
    }

    /**
     * 设置消费者信息列表
     *
     * @param consumers 消费者信息列表
     */
    public void setConsumers(List<ConsumerInfo> consumers) {
        this.consumers = consumers;
    }
}

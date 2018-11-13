package redis.clients.jedis.params.stream;

/**
 * 未确认消息信息
 * Created by WangXiao on 11/11/18.
 */
public class PendingInfo {

    /**
     * 消息Id
     */
    private String entryId;

    /**
     * 消费者名称
     */
    private String consumer;

    /**
     * 已闲置时间（分派后未确认的时间）
     */
    private long idle;

    /**
     * 重分派次数
     */
    private long deliveredTimes;

    /**
     * 取消息Id
     *
     * @return 消息Id
     */
    public String getEntryId() {
        return entryId;
    }

    /**
     * 设置消息Id
     *
     * @param entryId 消息Id
     */
    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    /**
     * 取消费者名称
     *
     * @return 消费者名称
     */
    public String getConsumer() {
        return consumer;
    }

    /**
     * 设置消费者名称
     *
     * @param consumer 消费者名称
     */
    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    /**
     * 取已闲置时间
     *
     * @return 已闲置时间
     */
    public long getIdle() {
        return idle;
    }

    /**
     * 设置已闲置时间
     *
     * @param idle 已闲置时间
     */
    public void setIdle(long idle) {
        this.idle = idle;
    }

    /**
     * 取重分派次数
     *
     * @return 重分派次数
     */
    public long getDeliveredTimes() {
        return deliveredTimes;
    }

    /**
     * 设置重分派次数
     *
     * @param deliveredTimes 重分派次数
     */
    public void setDeliveredTimes(long deliveredTimes) {
        this.deliveredTimes = deliveredTimes;
    }
}

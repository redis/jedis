package redis.clients.jedis.params.stream;

/**
 * ConsumerGroup的基本信息
 * Created by WangXiao on 11/11/18.
 */
public class GroupInfo {

    /**
     * ConsumerGroup的名字
     */
    private String name;

    /**
     * 消费者个数
     */
    private long consumers;

    /**
     * 已分配未结束的消息个数
     */
    private long pending;

    /**
     * 最后分配的消息的id
     */
    private String lastDeliveredId;

    /**
     * 取ConsumerGroup的名字
     *
     * @return ConsumerGroup的名字
     */
    public String getName() {
        return name;
    }

    /**
     * 设置ConsumerGroup的名字
     *
     * @param name ConsumerGroup的名字
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 取消费者个数
     *
     * @return 消费者个数
     */
    public long getConsumers() {
        return consumers;
    }

    /**
     * 设置消费者个数
     *
     * @param consumers 消费者个数
     */
    public void setConsumers(long consumers) {
        this.consumers = consumers;
    }

    /**
     * 取已分配未结束的消息个数
     *
     * @return 已分配未结束的消息个数
     */
    public long getPending() {
        return pending;
    }

    /**
     * 设置已分配未结束的消息个数
     *
     * @param pending 已分配未结束的消息个数
     */
    public void setPending(long pending) {
        this.pending = pending;
    }

    /**
     * 取最后分配的消息的id
     *
     * @return 最后分配的消息的id
     */
    public String getLastDeliveredId() {
        return lastDeliveredId;
    }

    /**
     * 设置最后分配的消息的id
     *
     * @param lastDeliveredId 最后分配的消息的id
     */
    public void setLastDeliveredId(String lastDeliveredId) {
        this.lastDeliveredId = lastDeliveredId;
    }
}

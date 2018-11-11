package redis.clients.jedis.params.stream;

/**
 * 消费者基本信息，可以通过XINFO CONSUMERS或XPENDING返回
 * Created by WangXiao on 11/11/18.
 */
public class ConsumerInfo {

    /**
     * 消费者名称
     */
    private String name;

    /**
     * 已分配未结束的消息数
     */
    private long pending;

    /**
     * 已闲置时间（未响应信息，没有XREADGROUP也没有XACK）
     */
    private long idle = -1;

    /**
     * 取消费者名称
     *
     * @return 消费者名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置消费者名称
     *
     * @param name 消费者名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 取已分配未结束的消息数
     *
     * @return 已分配未结束的消息数
     */
    public long getPending() {
        return pending;
    }

    /**
     * 设置已分配未结束的消息数
     *
     * @param pending 已分配未结束的消息数
     */
    public void setPending(long pending) {
        this.pending = pending;
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
}

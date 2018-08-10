package redis.clients.jedis.params.stream;

import com.sun.istack.internal.NotNull;

/**
 * 接收到的Stream新消息
 * Created by WangXiao on 8/10/18.
 */
public class NewStreamParams {

    /**
     * 收到消息的Stream键名
     */
    private String key;

    /**
     * 元素信息
     */
    private StreamParams entry;

    /**
     * 取键名
     * @return Stream键名
     */
    public String getKey() {
        return key;
    }

    /**
     * 设置键名
     * @param key 键名
     */
    public void setKey(@NotNull String key) {
        this.key = key;
    }

    /**
     * 取元素信息
     * @return 元素信息
     */
    public StreamParams getEntry() {
        return entry;
    }

    /**
     * 设置元素信息
     * @param entry 元素信息
     */
    public void setEntry(StreamParams entry) {
        this.entry = entry;
    }

    /**
     * 构造方法
     * @param key 键名
     * @param entry 元素信息
     */
    public NewStreamParams(String key, StreamParams entry){
        setKey(key);
        setEntry(entry);
    }
}

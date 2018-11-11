package redis.clients.jedis.params.stream;

/**
 * XINFO STREAM命令的返回信息
 * Created by WangXiao on 11/11/18.
 */
public class StreamInfo {

    /**
     * Stream的长度
     */
    private long length;

    /**
     * radix树的键个数
     */
    private long radixTreeKeys;

    /**
     * radix树的节点个数
     */
    private long radixTreeNodes;

    /**
     * 绑定的ConsumerGroup的个数
     */
    private long groups;

    /**
     * 最后一个元素的Id
     */
    private String lastGeneratedId;

    /**
     * 第一个元素
     */
    private StreamParams firstEntry;

    /**
     * 最后一个元素
     */
    private StreamParams lastEntry;

    /**
     * 取Stream长度
     *
     * @return Stream的长度
     */
    public long getLength() {
        return length;
    }

    /**
     * 设置Stream的长度
     *
     * @param length Stream的长度
     */
    public void setLength(long length) {
        this.length = length;
    }

    /**
     * 取radix树的键个数
     *
     * @return radix树的键个数
     */
    public long getRadixTreeKeys() {
        return radixTreeKeys;
    }

    /**
     * 设置radix树的键个数
     *
     * @param radixTreeKeys radix树的键个数
     */
    public void setRadixTreeKeys(long radixTreeKeys) {
        this.radixTreeKeys = radixTreeKeys;
    }

    /**
     * 取radix树的节点个数
     *
     * @return radix树的节点个数
     */
    public long getRadixTreeNodes() {
        return radixTreeNodes;
    }

    /**
     * 设置radix树的节点个数
     *
     * @param radixTreeNodes radix树的节点个数
     */
    public void setRadixTreeNodes(long radixTreeNodes) {
        this.radixTreeNodes = radixTreeNodes;
    }

    /**
     * 取绑定的ConsumerGroup的个数
     *
     * @return 绑定的ConsumerGroup的个数
     */
    public long getGroups() {
        return groups;
    }

    /**
     * 设置绑定的ConsumerGroup的个数
     *
     * @param groups 绑定的ConsumerGroup的个数
     */
    public void setGroups(long groups) {
        this.groups = groups;
    }

    /**
     * 取最后一个元素的Id
     *
     * @return 最后一个元素的Id
     */
    public String getLastGeneratedId() {
        return lastGeneratedId;
    }

    /**
     * 设置最后一个元素的Id
     *
     * @param lastGeneratedId 最后一个元素的Id
     */
    public void setLastGeneratedId(String lastGeneratedId) {
        this.lastGeneratedId = lastGeneratedId;
    }

    /**
     * 取第一个元素
     *
     * @return 第一个元素
     */
    public StreamParams getFirstEntry() {
        return firstEntry;
    }

    /**
     * 设置第一个元素
     *
     * @param firstEntry 第一个元素
     */
    public void setFirstEntry(StreamParams firstEntry) {
        this.firstEntry = firstEntry;
    }

    /**
     * 取最后一个元素
     *
     * @return 最后一个元素
     */
    public StreamParams getLastEntry() {
        return lastEntry;
    }

    /**
     * 设置最后一个元素
     *
     * @param lastEntry 最后一个元素
     */
    public void setLastEntry(StreamParams lastEntry) {
        this.lastEntry = lastEntry;
    }
}

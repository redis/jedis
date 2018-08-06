package redis.clients.jedis.params.stream;

import redis.clients.jedis.params.Params;

/**
 * Stream的元素
 * Created by WangXiao on 8/1/18.
 */
public class StreamParams extends Params {

    /**
     * 序号
     */
    private String entryId;

    /**
     * 取序号
     *
     * @return 序号
     */
    public String getEntryId() {
        return entryId;
    }

    /**
     * 设置序号
     *
     * @param entryId 序号
     */
    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    /**
     * 添加一个键值对
     *
     * @param field 字段名
     * @param value 值
     */
    public void addPair(String field, String value) {
        addParam(field, value);
    }
}
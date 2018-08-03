package redis.clients.jedis;

import redis.clients.jedis.params.stream.StreamParams;

import java.util.List;
import java.util.Map;

/**
 * Stream相关命令的定义
 * Created by WangXiao on 8/1/18.
 */
public interface StreamCommands {

    /**
     * 添加元素到Stream，采用entryId为*
     *
     * @param key     键名
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    String xaddDefault(String key, String entryId, String... pairs);

    /**
     * 添加元素到Stream，采用entryId为*
     *
     * @param key     键名
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    String xaddDefault(String key, Map<String, String> pairs);

    /**
     * 添加元素到Stream
     *
     * @param key     键名
     * @param entryId 序号，格式为xxx-xxx，只能增加，接受“*”
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    String xadd(String key, String entryId, String... pairs);

    /**
     * 添加元素到Stream
     *
     * @param key     键名
     * @param entryId 序号，格式为xxx-xxx，只能增加，接受“*”
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    String xadd(String key, String entryId, Map<String, String> pairs);

    /**
     * 添加元素到Stream，默认不使用“~”参数
     *
     * @param key     键名
     * @param maxLen  最大容量
     * @param entryId 序号，格式为xxx-xxx，只能增加，接受“*”
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    String xaddWithMaxlen(String key, long maxLen, String entryId, String... pairs);

    /**
     * 添加元素到Stream，默认不使用“~”参数
     *
     * @param key     键名
     * @param maxLen  最大容量
     * @param entryId 序号，格式为xxx-xxx，只能增加，接受“*”
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    String xaddWithMaxlen(String key, long maxLen, String entryId, Map<String, String> pairs);

    /**
     * 添加元素到Stream
     *
     * @param key     键名
     * @param approx  是否使用“~”参数
     * @param maxLen  最大容量
     * @param entryId 序号，格式为xxx-xxx，只能增加，接受“*”
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    String xaddWithMaxlen(String key, boolean approx, long maxLen, String entryId, String... pairs);

    /**
     * 添加元素到Stream
     *
     * @param key     键名
     * @param approx  是否使用“~”参数
     * @param maxLen  最大容量
     * @param entryId 序号，格式为xxx-xxx，只能增加，接受“*”
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    String xaddWithMaxlen(String key, boolean approx, long maxLen, String entryId, Map<String, String> pairs);

    /**
     * 取Stream的当前元素数
     *
     * @param key 键名
     * @return Stream当前元素数
     */
    long xlen(String key);

    /**
     * 取Stream中指定范围的元素，序号，接受“-”、“+”，相当于count值为0,取范围内全部
     *
     * @param key          键名
     * @param startEntryId 起始序号
     * @param endEntryId   结束序号
     * @return 元素集合
     */
    List<StreamParams> xrange(String key, String startEntryId, String endEntryId);

    /**
     * 取Stream中指定范围的元素，序号，接受“-”、“+”
     *
     * @param key          键名
     * @param startEntryId 起始序号
     * @param endEntryId   结束序号
     * @param count        最大取得的元素数量
     * @return 元素集合
     */
    List<StreamParams> xrange(String key, String startEntryId, String endEntryId, long count);

    /**
     * 反向取Stream中指定范围的元素，序号，接受“-”、“+”，相当于count值为0,取范围内全部
     *
     * @param key          键名
     * @param startEntryId 起始序号
     * @param endEntryId   结束序号
     * @return 元素集合
     */
    List<StreamParams> xrevrange(String key, String startEntryId, String endEntryId);

    /**
     * 反向取Stream中指定范围的元素，序号，接受“-”、“+”
     *
     * @param key          键名
     * @param startEntryId 起始序号
     * @param endEntryId   结束序号
     * @param count        最大取得的元素数量
     * @return 元素集合
     */
    List<StreamParams> xrevrange(String key, String startEntryId, String endEntryId, long count);
}
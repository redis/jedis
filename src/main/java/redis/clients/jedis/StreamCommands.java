package redis.clients.jedis;

import redis.clients.jedis.params.stream.*;

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
     * @param key   键名
     * @param pairs 键值对
     * @return 实际存储的序号
     */
    String xaddDefault(String key, String... pairs);

    /**
     * 添加元素到Stream，采用entryId为*
     *
     * @param key   键名
     * @param pairs 键值对
     * @return 实际存储的序号
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

    /**
     * 读取Stream中的数据
     *
     * @param params 键名组,起始序号ID组。ID接受“$”，但此时Redis会返回nil
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Map<String, List<StreamParams>> xread(String... params);

    /**
     * 读取Stream中的数据
     *
     * @param pairs 键名-起始序号ID的键值对。ID接受“$”，但此时Redis会返回nil
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Map<String, List<StreamParams>> xread(Map<String, String> pairs);

    /**
     * 读取Stream中的数据
     *
     * @param count  最大取得的元素数量
     * @param params 键名组,起始序号ID组。ID接受“$”，但此时Redis会返回nil
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Map<String, List<StreamParams>> xread(long count, String... params);

    /**
     * 读取Stream中的数据
     *
     * @param count 最大取得的元素数量
     * @param pairs 键名-起始序号ID的键值对。ID接受“$”，但此时Redis会返回nil
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Map<String, List<StreamParams>> xread(long count, Map<String, String> pairs);

    /**
     * 当使用BLOCK参数时，如果entryId不为“$”或大于最大元素的值，block参数不会生效，如果block生效，则只会返回最新的一个元素
     *
     * @param keys  键名
     * @param block 阻塞的时间，单位为毫秒
     * @return 取得的值, 只会有一个元素，Map的key为第一个取得了元素的Stream的键名。Redis可能返回nil，自动处理为空集合
     */
    NewStreamParams xreadBlock(long block, String... keys);

    /**
     * 一直Block直到接收到消息，即BLOCK 0
     *
     * @param keys 键名
     * @return 取得的值
     */
    NewStreamParams xreadBlock(String... keys);

    /**
     * 删除一个元素
     *
     * @param key     键名
     * @param entryId 序号
     * @return 删除的元素个数
     */
    long xdel(String key, String entryId);

    /**
     * 按最大容量裁剪Stream，默认不使用“~”参数
     *
     * @param key    键名
     * @param maxlen 最大容量
     * @return 删除的元素个数
     */
    long xtrimWithMaxlen(String key, long maxlen);

    /**
     * 按最大容量裁剪Stream
     *
     * @param key    键名
     * @param approx 是否使用“~”参数
     * @param maxlen 最大容量
     * @return 删除的元素个数
     */
    long xtrimWithMaxlen(String key, boolean approx, long maxlen);

    /**
     * 创建consumergroup，默认不使用“NOACK”参数
     *
     * @param key     读取的Stream键名
     * @param group   consumergroup的名字
     * @param entryId 读取Stream的起始序号
     * @return 成功返回“OK”，失败主要是由于Stream不存在，会返回“ERR no such key”
     */
    String xgroupcreate(String key, String group, String entryId);

    /**
     * 创建consumergroup
     *
     * @param key     读取的Stream键名
     * @param group   consumergroup的名字
     * @param entryId 读取Stream的起始序号
     * @param noack   是否使用“NOACK”参数
     * @return 成功返回“OK”，失败主要是由于Stream不存在，会返回“ERR no such key”
     */
    String xgroupcreate(String key, String group, String entryId, boolean noack);

    /**
     * 设置Consumer当前读取的游标
     *
     * @param key     Stream的键名
     * @param group   ConsumerGroup名
     * @param entryId 当前游标序号，接受“0”和“$”
     * @return 成功返回“OK”
     */
    String xgroupsetid(String key, String group, String entryId);

    /**
     * 销毁ConsumerGroup
     *
     * @param key   Stream的键名
     * @param group ConsumerGroup名
     * @return 删除的Group个数
     */
    long xgroupdestroy(String key, String group);


    /**
     * 从ConsumerGroup中删除一个consumer
     *
     * @param key      Stream的键名
     * @param group    ConsumerGroup名
     * @param consumer Consumer名
     * @return 未确认的消息数
     */
    long xgroupdelconsumer(String key, String group, String consumer);

    /**
     * 获取取Streams的基本信息
     *
     * @param key Stream的键名
     * @return Stream的基本信息封装
     */
    StreamInfo xinfostream(String key);

    /**
     * 获取与Stream绑定的ConsumerGroup信息
     *
     * @param key Stream的键名
     * @return Consumer信息列表
     */
    List<GroupInfo> xinfogroups(String key);

    /**
     * 获取指定ConsumerGroup中消费者信息列表
     *
     * @param key   Stream的键名
     * @param group ConsumerGroup的名称
     * @return 消费者信息列表
     */
    List<ConsumerInfo> xinfoconsumers(String key, String group);

}
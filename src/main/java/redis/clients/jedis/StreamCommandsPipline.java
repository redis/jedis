package redis.clients.jedis;

import redis.clients.jedis.params.stream.*;

import java.util.List;
import java.util.Map;

/**
 * Stream用于pipeline操作的接口
 * Created by WangXiao on 11/17/18.
 */
public interface StreamCommandsPipline {

    /**
     * 添加元素到Stream，采用entryId为*
     *
     * @param key   键名
     * @param pairs 键值对
     * @return 实际存储的序号
     */
    Response<String> xaddDefault(String key, String... pairs);

    /**
     * 添加元素到Stream，采用entryId为*
     *
     * @param key   键名
     * @param pairs 键值对
     * @return 实际存储的序号
     */
    Response<String> xaddDefault(String key, Map<String, String> pairs);

    /**
     * 添加元素到Stream
     *
     * @param key     键名
     * @param entryId 序号，格式为xxx-xxx，只能增加，接受“*”
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    Response<String> xadd(String key, String entryId, String... pairs);

    /**
     * 添加元素到Stream
     *
     * @param key     键名
     * @param entryId 序号，格式为xxx-xxx，只能增加，接受“*”
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    Response<String> xadd(String key, String entryId, Map<String, String> pairs);

    /**
     * 添加元素到Stream，默认不使用“~”参数
     *
     * @param key     键名
     * @param maxLen  最大容量
     * @param entryId 序号，格式为xxx-xxx，只能增加，接受“*”
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    Response<String> xaddWithMaxlen(String key, long maxLen, String entryId, String... pairs);

    /**
     * 添加元素到Stream，默认不使用“~”参数
     *
     * @param key     键名
     * @param maxLen  最大容量
     * @param entryId 序号，格式为xxx-xxx，只能增加，接受“*”
     * @param pairs   键值对
     * @return 实际存储的序号，如果输入的非“*”序号中不含“-”，Redis会自动添加“-0”并返回修改后的值
     */
    Response<String> xaddWithMaxlen(String key, long maxLen, String entryId, Map<String, String> pairs);

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
    Response<String> xaddWithMaxlen(String key, boolean approx, long maxLen, String entryId, String... pairs);

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
    Response<String> xaddWithMaxlen(String key, boolean approx, long maxLen, String entryId, Map<String, String> pairs);

    /**
     * 取Stream的当前元素数
     *
     * @param key 键名
     * @return Stream当前元素数
     */
    Response<Long> xlen(String key);

    /**
     * 取Stream中指定范围的元素，序号，接受“-”、“+”，相当于count值为0,取范围内全部
     *
     * @param key          键名
     * @param startEntryId 起始序号
     * @param endEntryId   结束序号
     * @return 元素集合
     */
    Response<List<StreamParams>> xrange(String key, String startEntryId, String endEntryId);

    /**
     * 取Stream中指定范围的元素，序号，接受“-”、“+”
     *
     * @param key          键名
     * @param startEntryId 起始序号
     * @param endEntryId   结束序号
     * @param count        最大取得的元素数量
     * @return 元素集合
     */
    Response<List<StreamParams>> xrange(String key, String startEntryId, String endEntryId, long count);

    /**
     * 反向取Stream中指定范围的元素，序号，接受“-”、“+”，相当于count值为0,取范围内全部
     *
     * @param key          键名
     * @param startEntryId 起始序号
     * @param endEntryId   结束序号
     * @return 元素集合
     */
    Response<List<StreamParams>> xrevrange(String key, String startEntryId, String endEntryId);

    /**
     * 反向取Stream中指定范围的元素，序号，接受“-”、“+”
     *
     * @param key          键名
     * @param startEntryId 起始序号
     * @param endEntryId   结束序号
     * @param count        最大取得的元素数量
     * @return 元素集合
     */
    Response<List<StreamParams>> xrevrange(String key, String startEntryId, String endEntryId, long count);

    /**
     * 读取Stream中的数据
     *
     * @param params 键名组,起始序号ID组。ID接受“$”，但此时Redis会返回nil
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Response<Map<String, List<StreamParams>>> xread(String... params);

    /**
     * 读取Stream中的数据
     *
     * @param pairs 键名-起始序号ID的键值对。ID接受“$”，但此时Redis会返回nil
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Response<Map<String, List<StreamParams>>> xread(Map<String, String> pairs);

    /**
     * 读取Stream中的数据
     *
     * @param count  最大取得的元素数量
     * @param params 键名组,起始序号ID组。ID接受“$”，但此时Redis会返回nil
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Response<Map<String, List<StreamParams>>> xread(long count, String... params);

    /**
     * 读取Stream中的数据
     *
     * @param count 最大取得的元素数量
     * @param pairs 键名-起始序号ID的键值对。ID接受“$”，但此时Redis会返回nil
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Response<Map<String, List<StreamParams>>> xread(long count, Map<String, String> pairs);

    /**
     * 删除一个元素
     *
     * @param key     键名
     * @param entryId 序号
     * @return 删除的元素个数
     */
    Response<Long> xdel(String key, String entryId);

    /**
     * 按最大容量裁剪Stream，默认不使用“~”参数
     *
     * @param key    键名
     * @param maxlen 最大容量
     * @return 删除的元素个数
     */
    Response<Long> xtrimWithMaxlen(String key, long maxlen);

    /**
     * 按最大容量裁剪Stream
     *
     * @param key    键名
     * @param approx 是否使用“~”参数
     * @param maxlen 最大容量
     * @return 删除的元素个数
     */
    Response<Long> xtrimWithMaxlen(String key, boolean approx, long maxlen);

    /**
     * 创建consumergroup，默认不使用“NOACK”参数
     *
     * @param key     读取的Stream键名
     * @param group   consumergroup的名字
     * @param entryId 读取Stream的起始序号
     * @return 成功返回“OK”，失败主要是由于Stream不存在，会返回“ERR no such key”
     */
    Response<String> xgroupcreate(String key, String group, String entryId);

    /**
     * 创建consumergroup
     *
     * @param key     读取的Stream键名
     * @param group   consumergroup的名字
     * @param entryId 读取Stream的起始序号
     * @param noack   是否使用“NOACK”参数
     * @return 成功返回“OK”，失败主要是由于Stream不存在，会返回“ERR no such key”
     */
    Response<String> xgroupcreate(String key, String group, String entryId, boolean noack);

    /**
     * 设置Consumer当前读取的游标
     *
     * @param key     Stream的键名
     * @param group   ConsumerGroup名
     * @param entryId 当前游标序号，接受“0”和“$”
     * @return 成功返回“OK”
     */
    Response<String> xgroupsetid(String key, String group, String entryId);

    /**
     * 销毁ConsumerGroup
     *
     * @param key   Stream的键名
     * @param group ConsumerGroup名
     * @return 删除的Group个数
     */
    Response<Long> xgroupdestroy(String key, String group);


    /**
     * 从ConsumerGroup中删除一个consumer
     *
     * @param key      Stream的键名
     * @param group    ConsumerGroup名
     * @param consumer Consumer名
     * @return 未确认的消息数
     */
    Response<Long> xgroupdelconsumer(String key, String group, String consumer);

    /**
     * 获取取Streams的基本信息
     *
     * @param key Stream的键名
     * @return Stream的基本信息封装
     */
    Response<StreamInfo> xinfostream(String key);

    /**
     * 获取与Stream绑定的ConsumerGroup信息
     *
     * @param key Stream的键名
     * @return Consumer信息列表
     */
    Response<List<GroupInfo>> xinfogroups(String key);

    /**
     * 获取指定ConsumerGroup中消费者信息列表
     *
     * @param key   Stream的键名
     * @param group ConsumerGroup的名称
     * @return 消费者信息列表
     */
    Response<List<ConsumerInfo>> xinfoconsumers(String key, String group);

    /**
     * 取ConsumerGroup的未确认消息信息
     *
     * @param key   Stream的键名
     * @param group ConsumerGroup的名称
     * @return ConsumerGroup未确认信息
     */
    Response<GroupPendingInfo> xpending(String key, String group);

    /**
     * 取ConsumerGroup下指定范围的所有消费者未确认消息的信息列表
     *
     * @param key          Stream的键名
     * @param group        ConsumerGroup的名称
     * @param startEntryId 起始的EntryId，接受“-”
     * @param endEntryId   最大的EntryId，接受“+”
     * @param count        最大可取的消息数
     * @return 未确认消息的信息列表
     */
    Response<List<PendingInfo>> xpending(String key, String group, String startEntryId, String endEntryId, long count);

    /**
     * 取ConsumerGroup下指定范围的指定消息者未确认消息的信息列表
     *
     * @param key          Stream的键名
     * @param group        ConsumerGroup的名称
     * @param startEntryId 起始的EntryId，接受“-”
     * @param endEntryId   最大的EntryId，接受“+”
     * @param count        最大可取的消息数
     * @param consumer     消费者名称
     * @return 指定消费者未确认消息的信息列表
     */
    Response<List<PendingInfo>> xpending(String key, String group, String startEntryId, String endEntryId, long count, String consumer);

    /**
     * 通过ConsumerGroup读取Stream中的数据
     *
     * @param group    ConsumerGroup名称
     * @param consumer 消费者名称
     * @param params   键名组,起始序号ID组。ID接受“>”
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, String... params);

    /**
     * 通过ConsumerGroup读取Stream中的数据
     *
     * @param group    ConsumerGroup名称
     * @param consumer 消费者名称
     * @param pairs    键名-起始序号ID的键值对。ID接受“>”
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, Map<String, String> pairs);

    /**
     * 通过ConsumerGroup读取Stream中的数据
     *
     * @param group    ConsumerGroup名称
     * @param consumer 消费者名称
     * @param count    最大取得的元素数量
     * @param params   键名组,起始序号ID组。ID接受“>”
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, long count, String... params);

    /**
     * 读取Stream中的数据
     *
     * @param group    ConsumerGroup名称
     * @param consumer 消费者名称
     * @param count    最大取得的元素数量
     * @param pairs    键名-起始序号ID的键值对。ID接受“>”
     * @return 每个Stream为一个从起始序号开始的全部元素，相当于count=0（允许redis返回nil，自动处理为空集），返回的是所有监听Stream的结果集的集合，以键名为key
     */
    Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, long count, Map<String, String> pairs);

    /**
     * 发送xack命令，用于结束消息
     *
     * @param key      消息所属的Stream键名
     * @param group    消息所属的ConsumerGroup组名
     * @param entryIds 消息Id组
     * @return 成功标记ACK的消息个数
     */
    Response<Long> xack(String key, String group, String... entryIds);

    /**
     * 发送xclaim命令，闲置时间为0
     *
     * @param key         Stream键名
     * @param group       consumerGroup名
     * @param consumer    消息者名称
     * @param minIdleTime 当满足闲置时间时进行写操作，用于实现分布式锁
     * @param entryIds    消息Id组
     * @return 分配的消息信息列表
     */
    Response<List<StreamParams>> xclaim(String key, String group, String consumer, long minIdleTime, String... entryIds);

    /**
     * 发送xclaim命令
     *
     * @param key         Stream键名
     * @param group       consumerGroup名
     * @param consumer    消息者名称
     * @param minIdleTime 当满足闲置时间时进行写操作，用于实现分布式锁
     * @param idleTime    将闲置时间设置为指定的毫秒数
     * @param entryIds    消息Id组
     * @return 分配的消息信息列表
     */
    Response<List<StreamParams>> xclaim(String key, String group, String consumer, long minIdleTime, long idleTime, String... entryIds);

    /**
     * 发送xclaim命令
     *
     * @param key         Stream键名
     * @param group       consumerGroup名
     * @param consumer    消息者名称
     * @param minIdleTime 当满足闲置时间时进行写操作，用于实现分布式锁
     * @param idleTime    将闲置时间设置为指定的毫秒数
     * @param entryIds    消息Id组
     * @return 分配的消息信息列表
     */
    Response<List<StreamParams>> xclaim(String key, String group, String consumer, long minIdleTime, long idleTime, long retryCount, String... entryIds);

    /**
     * 发送带FORCE参数的xclaim命令
     * <p>
     * 当使用FORCE标记时，会忽略minIdle的配置，，主要用于AOF和主从同步，创建不存在旧consumer的pending信息（首次分配），因此RETRYCOUNT应强制为1，TIME为当前时间戳
     *
     * @param key      Stream键名
     * @param group    consumerGroup名
     * @param consumer 消息者名称
     * @param entryIds 消息Id组
     * @return 分配的消息信息列表
     */
    Response<List<StreamParams>> xclaimForce(String key, String group, String consumer, String... entryIds);

    /**
     * 发送带JUSTID参数的xclaim命令
     *
     * @param key         Stream键名
     * @param group       consumerGroup名
     * @param consumer    消息者名称
     * @param minIdleTime 当满足闲置时间时进行写操作，用于实现分布式锁
     * @param entryIds    消息Id组
     * @return 分配的消息Id列表
     */
    Response<List<String>> xclaimJustid(String key, String group, String consumer, long minIdleTime, String... entryIds);

    /**
     * 发送带JUSTID参数的xclaim命令
     *
     * @param key         Stream键名
     * @param group       consumerGroup名
     * @param consumer    消息者名称
     * @param minIdleTime 当满足闲置时间时进行写操作，用于实现分布式锁
     * @param idleTime    将闲置时间设置为指定的毫秒数
     * @param entryIds    消息Id组
     * @return 分配的消息Id列表
     */
    Response<List<String>> xclaimJustid(String key, String group, String consumer, long minIdleTime, long idleTime, String... entryIds);

    /**
     * 发送带JUSTID参数的xclaim命令
     *
     * @param key         Stream键名
     * @param group       consumerGroup名
     * @param consumer    消息者名称
     * @param minIdleTime 当满足闲置时间时进行写操作，用于实现分布式锁
     * @param idleTime    将闲置时间设置为指定的毫秒数
     * @param entryIds    消息Id组
     * @return 分配的消息Id列表
     */
    Response<List<String>> xclaimJustid(String key, String group, String consumer, long minIdleTime, long idleTime, long retryCount, String... entryIds);

    /**
     * 发送带FORCE和JUSTID参数的xclaim命令
     * <p>
     * 当使用FORCE标记时，会忽略minIdle的配置，，主要用于AOF和主从同步，创建不存在旧consumer的pending信息（首次分配），因此RETRYCOUNT应强制为1，TIME为当前时间戳
     *
     * @param key      Stream键名
     * @param group    consumerGroup名
     * @param consumer 消息者名称
     * @param entryIds 消息Id组
     * @return 分配的消息Id列表
     */
    Response<List<String>> xclaimForceAndJustid(String key, String group, String consumer, String... entryIds);
}

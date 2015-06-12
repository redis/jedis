/**
 * 
 */
package redis.clients.jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import redis.clients.util.JedisClusterCRC16;
import redis.clients.util.SafeEncoder;

/**
 * @ClassName: CustomizeClusterPipeline
 * @Description: TODO 自定义 CustomizeClusterPipeline ,增加 cluster pipeline 功能
 * @author xiongshengjie 382202220@qq.com
 * @date 2015年6月9日 下午3:28:40
 * 
 */
public class CustomizeClusterPipeline extends MultiKeyPipelineBase {

	private Queue<Client> clients = new LinkedList<Client>();

	private JedisClusterConnectionHandler jedisClusterConnectionHandler;

	private Map<String, Jedis> nodeMap = new HashMap<String, Jedis>();

	/**
	 * @param key
	 * @return
	 * @date 2015年6月9日
	 * @author xiongshengjie
	 * @override @see
	 *           redis.clients.jedis.PipelineBase#getClient(java.lang.String)
	 * @Description TODO
	 */
	@Override
	protected Client getClient(String key)
	{
		byte[] keys = SafeEncoder.encode(key);
		return this.getClient(keys);
	}

	/**
	 * @param key
	 * @return
	 * @date 2015年6月9日
	 * @author xiongshengjie
	 * @override @see redis.clients.jedis.PipelineBase#getClient(byte[])
	 * @Description TODO
	 */
	@Override
	protected Client getClient(byte[] keys)
	{
		return this.getNodeBySlot(keys);
	}

	/**
	 * @param keys
	 * @return
	 * @date 2015年6月10日
	 * @author xiongshengjie
	 * @return Client
	 * @Description 根据byte[] keys 获取对应的cluster node
	 */
	private Client getNodeBySlot(byte[] keys)
	{
		String currenthostport = null;

		int slot = JedisClusterCRC16.getSlot(keys);

		JedisPool jedisPool = jedisClusterConnectionHandler.cache.getSlotPool(slot);

		Map<String, JedisPool> map = jedisClusterConnectionHandler.cache.getNodes();

		for (Iterator<String> it = map.keySet().iterator(); it.hasNext();)
		{
			String hostport = it.next();
			if (map.get(hostport).equals(jedisPool))
			{
				currenthostport = hostport;
				break;
			}
		}

		Jedis jedis = null;

		if (nodeMap.containsKey(currenthostport))
		{
			jedis = nodeMap.get(currenthostport);
		} else
		{
			jedis = jedisClusterConnectionHandler.getConnectionFromSlot(slot);
			nodeMap.put(currenthostport, jedis);
		}

		Client client = jedis.getClient();

		clients.add(client);

		return client;
	}

	/**
	 * @return
	 * @date 2015年6月10日
	 * @author xiongshengjie
	 * @return List<Object>
	 * @Description 获取批量执行后所有的返回结果
	 */
	public List<Object> getAllResponeseList()
	{
		List<Object> result = new ArrayList<Object>();
		for (Client client : clients)
		{
			result.add(generateResponse(client.getOne()).get());
		}
		return result;
	}

	/**
	 * 
	 * @date 2015年6月11日
	 * @author xiongshengjie
	 * @return void
	 * @Description TODO 关闭当前管道中的连接
	 */
	public void pipelineClose()
	{
		if (!nodeMap.isEmpty())
		{
			Iterator<String> it = nodeMap.keySet().iterator();
			for (; it.hasNext();)
			{
				String hostport = it.next();
				nodeMap.get(hostport).close();
				it.remove();
			}
		}
	}

	public JedisClusterConnectionHandler getJedisClusterConnectionHandler()
	{
		return jedisClusterConnectionHandler;
	}

	public void setJedisClusterConnectionHandler(JedisClusterConnectionHandler jedisClusterConnectionHandler)
	{
		this.jedisClusterConnectionHandler = jedisClusterConnectionHandler;
	}

}

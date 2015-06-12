/**
 * 
 */
package redis.clients.jedis.tests.benchmark;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.CustomizeClusterPipeline;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.PipelineBase;
import redis.clients.jedis.Response;

/**
 * @ClassName: Cluster
 * @Description: TODO
 * @author xiongshengjie 382202220@qq.com
 * @date 2015年6月1日 下午5:24:25
 * 
 */
public class ClusterTest {

	public static int COUNT = 10000;
	public static Set<HostAndPort> nodes = new HashSet<HostAndPort>();
	public static GenericObjectPoolConfig config = new GenericObjectPoolConfig();
	public JedisCluster cluster = null ;
	@BeforeClass
	public static void init()
	{

		config.setMaxTotal(10000);
		config.setMinIdle(1000);
		config.setMaxIdle(10000);// 对象最大空闲时间
		config.setMaxWaitMillis(1000 * 20);// 获取对象时最大等待时间

		String[] hostAndPorts = { "132.121.88.81:6371", "132.121.88.81:6372", "132.121.88.81:6373", "132.121.88.81:6374", "132.121.88.81:6375", "132.121.88.81:6376" };
		for (String hostAndPort : hostAndPorts)
		{
			String[] nodeHostAndPort = hostAndPort != null ? hostAndPort.split(":") : new String[] {};
			if (nodeHostAndPort.length == 2)
			{
				String nodeHost = nodeHostAndPort[0];
				Integer nodePort = Integer.valueOf(nodeHostAndPort[1]);
				HostAndPort node = new HostAndPort(nodeHost, nodePort > 0 ? nodePort : 6379);
				nodes.add(node);
			}
		}
	}
	@Before
	public void setup()
	{
	    cluster = new JedisCluster(nodes, 100000, config);
	}
	@After
	public void teardown() throws IOException
	{
		cluster.close();
	}
	
	

	@Test
	public  void testClusterSetTime()
	{
		long start = System.currentTimeMillis();
		for (int i = 0; i < COUNT; i++)
		{
			cluster.set("key" + i, "test111111111111111111111111111111111111111111111111111111111111111111");
		}
		System.out.println("cluster need time : " + (System.currentTimeMillis() - start) + " millsec");

	}

	@Test
	public  void testCustomizeClusterPipelineSetTime()
	{
		CustomizeClusterPipeline pipeline = cluster.pipelined();
		long start = System.currentTimeMillis();
		for (int i = 0; i < COUNT; i++)
		{
			pipeline.set("key" + i, "test111111111111111111111111111111111111111111111111111111111111111111");
		}

		List<Object> responseList = pipeline.getAllResponeseList();
		pipeline.pipelineClose();
		System.out.println("clusterPipeline need time : " + (System.currentTimeMillis() - start) + " millsec");

	}

	

}

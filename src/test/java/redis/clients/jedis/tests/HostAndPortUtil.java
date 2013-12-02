package redis.clients.jedis.tests;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;

public class HostAndPortUtil {
    private static List<HostAndPort> redisHostAndPortList = new ArrayList<HostAndPort>();
    private static List<HostAndPort> sentinelHostAndPortList = new ArrayList<HostAndPort>();

    static {
    	
        HostAndPort defaulthnp1 = new HostAndPort("localhost", Protocol.DEFAULT_PORT);
        redisHostAndPortList.add(defaulthnp1);

        HostAndPort defaulthnp2 = new HostAndPort("localhost", Protocol.DEFAULT_PORT + 1);
        redisHostAndPortList.add(defaulthnp2);
        
        HostAndPort defaulthnp3 = new HostAndPort("localhost", Protocol.DEFAULT_PORT + 2);
        redisHostAndPortList.add(defaulthnp3);
        
        HostAndPort defaulthnp4 = new HostAndPort("localhost", Protocol.DEFAULT_PORT + 3);
        redisHostAndPortList.add(defaulthnp4);
        
        HostAndPort defaulthnp5 = new HostAndPort("localhost", Protocol.DEFAULT_PORT + 4);
        redisHostAndPortList.add(defaulthnp5);
        
        HostAndPort defaulthnp6 = new HostAndPort("localhost", Protocol.DEFAULT_PORT + 5);
        redisHostAndPortList.add(defaulthnp6);
        
        HostAndPort defaulthnp7 = new HostAndPort("localhost", Protocol.DEFAULT_SENTINEL_PORT);
        sentinelHostAndPortList.add(defaulthnp7);
        
        HostAndPort defaulthnp8 = new HostAndPort("localhost", Protocol.DEFAULT_SENTINEL_PORT + 1);
        sentinelHostAndPortList.add(defaulthnp8);
        
        HostAndPort defaulthnp9 = new HostAndPort("localhost", Protocol.DEFAULT_SENTINEL_PORT + 2);
        sentinelHostAndPortList.add(defaulthnp9);

        String envRedisHosts = System.getProperty("redis-hosts");
        String envSentinelHosts = System.getProperty("sentinel-hosts");
        
        redisHostAndPortList = parseHosts(envRedisHosts, redisHostAndPortList);                        
        sentinelHostAndPortList = parseHosts(envSentinelHosts, sentinelHostAndPortList);        
    }

    public static List<HostAndPort> parseHosts(String envHosts, List<HostAndPort> existingHostsAndPorts) {
    	
    	if (null != envHosts && 0 < envHosts.length()) {
        	
            String[] hostDefs = envHosts.split(",");
            
            if (null != hostDefs && 2 <= hostDefs.length) {
            	
            	List<HostAndPort> envHostsAndPorts = new ArrayList<HostAndPort>(hostDefs.length);
                
                for (String hostDef : hostDefs) {
                                    	
                	String[] hostAndPort = hostDef.split(":");
                	
                    if (null != hostAndPort && 2 == hostAndPort.length) {
                    	String host = hostAndPort[0];
                    	int port = Protocol.DEFAULT_PORT;
                    	
                    	try {
                            port = Integer.parseInt(hostAndPort[1]);
                        } catch (final NumberFormatException nfe) {
                        }
                    	
                        envHostsAndPorts.add(new HostAndPort(host, port));
                    }
                }
                                
                return envHostsAndPorts;
            }
        }
    	
    	return existingHostsAndPorts;
    }
    
    public static List<HostAndPort> getRedisServers() {
        return redisHostAndPortList;
    }
    
    public static List<HostAndPort> getSentinelServers() {
        return sentinelHostAndPortList;
    }

}

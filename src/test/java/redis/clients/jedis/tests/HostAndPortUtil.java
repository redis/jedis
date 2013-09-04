package redis.clients.jedis.tests;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Protocol;

public class HostAndPortUtil {
    private static List<HostAndPort> redisHostAndPortList = new ArrayList<HostAndPortUtil.HostAndPort>();
    private static List<HostAndPort> sentinelHostAndPortList = new ArrayList<HostAndPortUtil.HostAndPort>();

    static {
    	
        HostAndPort defaulthnp1 = new HostAndPort();
        defaulthnp1.host = "localhost";
        defaulthnp1.port = Protocol.DEFAULT_PORT;        
        redisHostAndPortList.add(defaulthnp1);

        HostAndPort defaulthnp2 = new HostAndPort();
        defaulthnp2.host = "localhost";
        defaulthnp2.port = Protocol.DEFAULT_PORT + 1;
        redisHostAndPortList.add(defaulthnp2);
        
        HostAndPort defaulthnp3 = new HostAndPort();
        defaulthnp3.host = "localhost";
        defaulthnp3.port = Protocol.DEFAULT_PORT + 2;
        redisHostAndPortList.add(defaulthnp3);
        
        HostAndPort defaulthnp4 = new HostAndPort();
        defaulthnp4.host = "localhost";
        defaulthnp4.port = Protocol.DEFAULT_PORT + 3;
        redisHostAndPortList.add(defaulthnp4);
        
        HostAndPort defaulthnp5 = new HostAndPort();
        defaulthnp5.host = "localhost";
        defaulthnp5.port = Protocol.DEFAULT_SENTINEL_PORT;
        sentinelHostAndPortList.add(defaulthnp5);
        
        HostAndPort defaulthnp6 = new HostAndPort();
        defaulthnp6.host = "localhost";
        defaulthnp6.port = Protocol.DEFAULT_SENTINEL_PORT + 1;
        sentinelHostAndPortList.add(defaulthnp6);
        
        HostAndPort defaulthnp7 = new HostAndPort();
        defaulthnp7.host = "localhost";
        defaulthnp7.port = Protocol.DEFAULT_SENTINEL_PORT + 2;
        sentinelHostAndPortList.add(defaulthnp7);

        String envRedisHosts = System.getProperty("redis-hosts");
        String envSentinelHosts = System.getProperty("sentinel-hosts");
        
        redisHostAndPortList = parseHosts(envRedisHosts, redisHostAndPortList);                        
        sentinelHostAndPortList = parseHosts(envSentinelHosts, sentinelHostAndPortList);        
    }

    public static List<HostAndPort> parseHosts(String envHosts, List<HostAndPort> existingHostsAndPorts) {
    	
    	if (null != envHosts && 0 < envHosts.length()) {
        	
            String[] hostDefs = envHosts.split(",");
            
            if (null != hostDefs && 2 <= hostDefs.length) {
            	
            	List<HostAndPort> envHostsAndPorts = new ArrayList<HostAndPortUtil.HostAndPort>(hostDefs.length);
                
                for (String hostDef : hostDefs) {
                                    	
                	String[] hostAndPort = hostDef.split(":");
                	
                    if (null != hostAndPort && 2 == hostAndPort.length) {
                    	
                        HostAndPort hnp = new HostAndPort();
                        hnp.host = hostAndPort[0];
                        
                        try {
                            hnp.port = Integer.parseInt(hostAndPort[1]);
                        } catch (final NumberFormatException nfe) {
                            hnp.port = Protocol.DEFAULT_PORT;
                        }                                                
                        
                        envHostsAndPorts.add(hnp);
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

    public static class HostAndPort {
        public String host;
        public int port;
        
        @Override
        public String toString() {
        	return host + ":" + port;
        }
    }
}

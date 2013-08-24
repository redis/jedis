package redis.clients.jedis;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.pool.impl.GenericObjectPool.Config;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.util.Pool;

public class JedisSentinelPool extends Pool<Jedis> {

    protected Config poolConfig;        
    
    protected int timeout = Protocol.DEFAULT_TIMEOUT;
    
    protected String password;
    
    protected int database = Protocol.DEFAULT_DATABASE;

    protected Logger log = Logger.getLogger(getClass().getName());
    
    public JedisSentinelPool(String masterName, Set<String> sentinels, final Config poolConfig) {
        this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisSentinelPool(String masterName, Set<String> sentinels) {
        this(masterName, sentinels, new Config(), Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }        
    
    public JedisSentinelPool(String masterName, Set<String> sentinels, final Config poolConfig, int timeout, final String password) {
        this(masterName, sentinels, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE);
    }
    
    public JedisSentinelPool(String masterName, Set<String> sentinels, final Config poolConfig, final int timeout) {
        this(masterName, sentinels, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisSentinelPool(String masterName, Set<String> sentinels, final Config poolConfig, int timeout, final String password,
                     final int database) {
    	this.poolConfig = poolConfig;
    	HostAndPort master = initSentinels(sentinels, masterName);
        initPool(master);    	        
    }

    public void returnBrokenResource(final BinaryJedis resource) {
    	returnBrokenResourceObject(resource);
    }
    
    public void returnResource(final BinaryJedis resource) {
    	returnResourceObject(resource);
    }
    
    private class HostAndPort {
        String host;
        int port;

        @Override
        public boolean equals(Object obj) {
          if (obj instanceof HostAndPort) {
            final HostAndPort that = (HostAndPort) obj;
            return this.port == that.port && this.host.equals(that.host);
          }
          return false;
        }
        
        @Override
        public String toString() {
          return host + ":" + port;
        }
      }

      public class JedisPubSubAdapter extends JedisPubSub {

        @Override
        public void onMessage(String channel, String message) {

        }

        @Override
        public void onPMessage(String pattern, String channel, String message) {

        }

        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {

        }

        @Override
        public void onPUnsubscribe(String pattern, int subscribedChannels) {

        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {

        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {

        }
      }

      
      private volatile HostAndPort currentHostMaster;

      public void destroy() {
        // FIXME clean Sentinel connections
        super.destroy();
      }

      public HostAndPort getCurrentHostMaster() {
        return currentHostMaster;
      }

      private void initPool(HostAndPort master) {
        if (!master.equals(currentHostMaster)) {
          currentHostMaster = master;
          log("Created pool: " + master);          
          initPool(poolConfig, new JedisFactory(master.host, master.port, timeout, password, database));          
        }
      }

      private HostAndPort initSentinels(Set<String> sentinels, final String masterName) {
        HostAndPort master = null;
        final boolean running = true;
        outer: while (running) {
          for (String sentinel : sentinels) {
            final HostAndPort hap = toHostAndPort(Arrays.asList(sentinel.split(":")));
            try {
              final Jedis jedis = new Jedis(hap.host, hap.port);
              if (master == null) {
                master = toHostAndPort(jedis.sentinelGetMasterAddrByName(masterName));
                jedis.disconnect();
                break outer;
              }
            } catch (JedisConnectionException e) {
              log("Cannot connect to sentinel running @ " + hap + ". Trying next one.");
            }
          }
          try {
            log("All sentinels down, cannot determinate where is " + masterName + " master is running... sleeping 1000ms.");
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        log("Got master running at " + master + ". Starting sentinel listeners.");
        for (String sentinel : sentinels) {
          final HostAndPort hap = toHostAndPort(Arrays.asList(sentinel.split(":")));

          new Thread() {
            public void run() {
              boolean running = true;
              while (running) {
                final Jedis jedis = new Jedis(hap.host, hap.port);
                try {
                  jedis.subscribe(new JedisPubSubAdapter() {
                    @Override
                    public void onMessage(String channel, String message) {
                      // System.out.println(channel + ": " + message);
                      // +switch-master: mymaster 127.0.0.1 6379 127.0.0.1 6380
                      log("Sentinel " + hap + " published: " + message + ".");
                      final String[] switchMasterMsg = message.split(" ");
                      if (masterName.equals(switchMasterMsg[0])) {
                        initPool(toHostAndPort(Arrays.asList(switchMasterMsg[3], switchMasterMsg[4])));
                      }
                    }
                  }, "+switch-master");
                } catch (JedisConnectionException e) {
                  log("Lost connection to " + hap + ". Sleeping 5000ms.");
                  try {
                    Thread.sleep(5000);
                  } catch (InterruptedException e1) {
                    e1.printStackTrace();
                  }
                }
              }
            };
          }.start();
        }

        return master;
      }

      // sophisticated logging
      private void log(String msg) {
        log.info(msg);
      }

      private HostAndPort toHostAndPort(List<String> getMasterAddrByNameResult) {
        final HostAndPort hap = new HostAndPort();
        hap.host = getMasterAddrByNameResult.get(0);
        hap.port = Integer.parseInt(getMasterAddrByNameResult.get(1));
        return hap;
      }
    
}

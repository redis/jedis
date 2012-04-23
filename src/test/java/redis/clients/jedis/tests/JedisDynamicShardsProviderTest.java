package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;
import redis.clients.util.JedisDynamicShardsProvider;

public class JedisDynamicShardsProviderTest {
    private static HostAndPort redis1 =
    	HostAndPortUtil.getRedisServers().get(0);
    
    private static HostAndPort redis2 =
    	HostAndPortUtil.getRedisServers().get(1);
    
    private static final String K1 = "a";
    private static final String K2 = "b";
    private static final String V1 = "foo";
    private static final String V2 = "bar";

    private static final String V1ONR1 = "foo on r1";
    private static final String V1ONR2 = "foo on r2";
    private static final String V2ONR1 = "bar on r1";
    private static final String V2ONR2 = "bar on r2";

	@Test
	public void testDynamicShardsUpdate() {
		
		final JedisShardInfo shard1 = new JedisShardInfo(redis1.host, redis1.port);
		shard1.setPassword("foobared");
		final JedisShardInfo shard2 = new JedisShardInfo(redis2.host, redis2.port);
		shard2.setPassword("foobared");
		
		final ArrayList<JedisShardInfo> initialShards = new ArrayList<JedisShardInfo>(2);
		initialShards.add(shard1);
		initialShards.add(shard2);
		
		final JedisDynamicShardsProvider provider = new JedisDynamicShardsProvider(initialShards);
		final ShardedJedis shardedJedis = new ShardedJedis(provider);
		
		// Push the keys 'a' & 'b'
		shardedJedis.set(K1, V1);
        shardedJedis.set(K2, V2);

        JedisShardInfo s1 = shardedJedis.getShardInfo(K1);
        JedisShardInfo s2 = shardedJedis.getShardInfo(K2);

        assertNotSame(s1, s2);
        checkKeysAreNotOnSameRedisServer(s1, s2);

        // Remove the keys
        shardedJedis.del(K1);
        shardedJedis.del(K2);
        checkKeysDoNotExist(s1, s2);
        
        // Remove a shard ...
        provider.removeShard(shard2);

        // Push the keys 'a' & 'b' again
		shardedJedis.set(K1, V1);
        shardedJedis.set(K2, V2);

        s1 = shardedJedis.getShardInfo(K1);
        s2 = shardedJedis.getShardInfo(K2);

        assertSame(s1, s2);
        checkKeysAreOnSameRedisServer(s1);

        // Remove the keys
        shardedJedis.del(K1);
        shardedJedis.del(K2);
        checkKeysDoNotExist(s1, s2);
        
        shardedJedis.disconnect();
	}
	
	@Test
	public void testConcurrentReadsAndProviderUpdate() throws InterruptedException {
		final JedisShardInfo shard1 = new JedisShardInfo(redis1.host, redis1.port);
		shard1.setPassword("foobared");
		final JedisShardInfo shard2 = new JedisShardInfo(redis2.host, redis2.port);
		shard2.setPassword("foobared");
		
		final ArrayList<JedisShardInfo> initialShards = new ArrayList<JedisShardInfo>(2);
		initialShards.add(shard1);
		initialShards.add(shard2);
		
		final JedisDynamicShardsProvider provider = new JedisDynamicShardsProvider(initialShards);


		final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
		final Lock readLock = rwLock.readLock();
		final Lock writeLock = rwLock.writeLock();

		
		Jedis j = null;

        // Put both keys everywhere ...
        j = new Jedis(shard1.getHost(), shard1.getPort());
        j.auth("foobared");
        j.set(K1, V1ONR1);
        j.set(K2, V2ONR1);
        j.disconnect();

 
        j = new Jedis(shard2.getHost(), shard2.getPort());
        j.auth("foobared");
        j.set(K1, V1ONR2);
        j.set(K2, V2ONR2);
        j.disconnect();

		final ShardedJedis[] shardedJedis = new ShardedJedis[5];
		for(int i = 0; i < 5; i++) {
			shardedJedis[i] = new ShardedJedis(provider);
		}

		ReaderRunnable[] readers = new ReaderRunnable[5];
        for(int i=0; i < 5; i++) {
        	readers[i] = new ReaderRunnable(provider, shardedJedis[i], readLock);
        }
        
        ProviderRunnable pr1 = new ProviderRunnable(provider, writeLock);
        
        Thread[] readersThread = new Thread[5];
        for(int i=0; i < 5; i++) {
        	readersThread[i] = new Thread(readers[i]);
        }
        
        Thread tpr1 = new Thread(pr1);
        
        for(int i=0; i < 5; i++) {
        	readersThread[i].start();
        }
        tpr1.start();

        Thread.currentThread().sleep(5000);
        
        for(int i=0; i < 5; i++) {
        	readers[i].setStop();
        }
        pr1.setStop();
        
        for(int i=0; i < 5; i++) {
        	readersThread[i].join();
        }
        tpr1.join();
        
        for(int i=0; i < 5; i++) {
        	shardedJedis[i].disconnect();
        }

        // Cleanup
        j = new Jedis(shard1.getHost(), shard1.getPort());
        j.auth("foobared");
        j.del(K1);
        j.del(K2);
        j.disconnect();
 
        j = new Jedis(shard2.getHost(), shard2.getPort());
        j.auth("foobared");
        j.del(K1);
        j.del(K2);
        j.disconnect();

        
        StringBuilder failedMesg = new StringBuilder();
        for(int i=0; i < 5; i++) {
        	if(readers[i].hasFailed()) {
        		failedMesg
        			.append("\nReader #")
        			.append(i)
        			.append(" failed : [Total:")
        			.append(readers[i].getTotal())
        			.append(", Mismatch:")
        			.append(readers[i].getMismatch())
        			.append("]");
        	} else if(0 != readers[i].getMismatch()){
        		failedMesg
    			.append("\nReader #")
    			.append(i)
    			.append(" didn't failed BUT : [Total:")
    			.append(readers[i].getTotal())
    			.append(", Mismatch:")
    			.append(readers[i].getMismatch())
    			.append("]");
        	}
        }
        
        if(0 != failedMesg.length()) {
        	fail(failedMesg.toString());
        }
	}
	
	private void checkKeysAreNotOnSameRedisServer(final JedisShardInfo s1, //
											 	  final JedisShardInfo s2) {
        Jedis j = null;
        j = new Jedis(s1.getHost(), s1.getPort());
        j.auth("foobared");
        assertEquals(V1, j.get(K1));
        j.disconnect();

        j = new Jedis(s2.getHost(), s2.getPort());
        j.auth("foobared");
        assertEquals(V2, j.get(K2));
        j.disconnect();
	}
	
	private void checkKeysDoNotExist(final JedisShardInfo s1, //
			 						 final JedisShardInfo s2) {
        Jedis j = null;
        j = new Jedis(s1.getHost(), s1.getPort());
        j.auth("foobared");
        assertFalse(j.exists(K1));
        assertFalse(j.exists(K2));
        j.disconnect();

        j = new Jedis(s2.getHost(), s2.getPort());
        j.auth("foobared");
        assertFalse(j.exists(K1));
        assertFalse(j.exists(K2));
        j.disconnect();
	}

	private void checkKeysAreOnSameRedisServer(final JedisShardInfo s1) {
		Jedis j = null;
		j = new Jedis(s1.getHost(), s1.getPort());
		j.auth("foobared");
		assertEquals(V1, j.get(K1));
		assertEquals(V2, j.get(K2));
		j.disconnect();
	}
	
	/////////////////////////////////////////
	//   Inner classes for concurrent tests
	/////////////////////////////////////////
	
	private static class ReaderRunnable implements Runnable {
		private final JedisDynamicShardsProvider provider;
		private final ShardedJedis shardedJedis;
		private final Lock readLock;
		private boolean stop = false;
		private long total = 0;
		private long mismatch = 0;
		private boolean failed = false;
		

		public ReaderRunnable(	final JedisDynamicShardsProvider provider, //
								final ShardedJedis shardedJedis,
								final Lock readLock) {
			this.provider = provider;
			this.shardedJedis = shardedJedis;
			this.readLock = readLock;
		}
		public void setStop() {
			this.stop = true;
		}
		
		public long getTotal() {
			return total;
		}
		
		public long getMismatch() {
			return mismatch;
		}
		
		public boolean hasFailed() {
			return failed;
		}

		public void run() {
			String result1 = null;
			String result2 = null;
			JedisShardInfo s1 = null;
			JedisShardInfo s2 = null;
			int initialShardSize = 0;
			while(!stop) {
				readLock.lock();
				try {
					// Don't want a write to occur ...
					// So using a lock in order to have "atomic" execution for the 3 operations below
					// but this allow multiple concurrent read ...
					initialShardSize = provider.getShards().size();
					result1 = shardedJedis.get(K1);
					result2 = shardedJedis.get(K2);
				} finally {
					readLock.unlock();
				}
				
				if(1 == initialShardSize) {
					// If there is only 1 shard, we are expecting :
					// result1 == V1ONR1 && result2 == V2ONR1
					// OR
					// result1 == V1ONR2 && result2 == V2ONR2
					if( !(
							(V1ONR1.equals(result1) && V2ONR1.equals(result2))
							||
							(V1ONR2.equals(result1) && V2ONR2.equals(result2))
						)
					) {
						mismatch++;
//						if(initialShardSize == finalShardSize) {
							failed = true;
							stop = true;
//						}
					}
				} else {
					// If there are 2 shards, we are expecting:
					// result1 == V1ONR1 && result2 == V2ONR2
					// OR
					// result1 == V1ONR2 && result2 == V2ONR1
					if(!(
							(V1ONR1.equals(result1) && V2ONR2.equals(result2))
							||
							(V1ONR2.equals(result1) && V2ONR1.equals(result2))
						)
					) {
						mismatch++;
//						if(initialShardSize == finalShardSize) {
							failed = true;
							stop = true;
//						}
					}
				}

				total++;
			}
		}
		
	}

	private static class ProviderRunnable implements Runnable {
		private final JedisDynamicShardsProvider provider;
		private final Lock writeLock;
		private boolean stop = false;

		public ProviderRunnable(final JedisDynamicShardsProvider provider, //
								final Lock writeLock){
			this.provider = provider;
			this.writeLock = writeLock;
		}
		public void setStop() {
			this.stop = true;
		}

		public void run() {
			JedisShardInfo s1 = provider.getShards().get(0);
			JedisShardInfo s2 = provider.getShards().get(1);
			while(!stop) {
				try {
					Thread.currentThread().sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				writeLock.lock();
				try {
					provider.setShards(Arrays.asList(s1));
				} finally {
					writeLock.unlock();
				}

				try {
					Thread.currentThread().sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				writeLock.lock();
				try {
					provider.setShards(Arrays.asList(s2));
				} finally {
					writeLock.unlock();
				}

				try {
					Thread.currentThread().sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				writeLock.lock();
				try {
					provider.setShards(Arrays.asList(s1, s2));
				} finally {
					writeLock.unlock();
				}
			}
		}
	}
}

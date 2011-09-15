package redis.clients.jedis.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import redis.clients.util.AbstractDynamicShardsProvider;
import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;


public class DynamicShardsProviderTest {
	@Test
	public void testShardUpdatesWithoutRedisServer() throws InterruptedException {
		DummyDynamicShardsProvider undertest = new DummyDynamicShardsProvider();
		List<DummyShardInfo> shards = new ArrayList<DynamicShardsProviderTest.DummyShardInfo>();
		shards.add(new DummyShardInfo("Shard1"));
		shards.add(new DummyShardInfo("Shard2"));
		shards.add(new DummyShardInfo("Shard3"));

		undertest.setShards(shards);

		DummySharded sharded = new DummySharded(undertest);
		undertest.register(sharded);
		
		ReadRunnable rr1 = new ReadRunnable(shards, sharded);
		ReadRunnable rr2 = new ReadRunnable(shards, sharded);
		ReadRunnable rr3 = new ReadRunnable(shards, sharded);
		ReadRunnable rr4 = new ReadRunnable(shards, sharded);
		ReadRunnable rr5 = new ReadRunnable(shards, sharded);

		WriteRunnable wr = new WriteRunnable(undertest);
		
		Thread tr1 = new Thread(rr1);
		Thread tr2 = new Thread(rr2);
		Thread tr3 = new Thread(rr3);
		Thread tr4 = new Thread(rr4);
		Thread tr5 = new Thread(rr5);

		Thread tw = new Thread(wr);
		
		tr1.start();
		tr2.start();
		tr3.start();
		tr4.start();
		tr5.start();

		tw.start();
		
		Thread.currentThread().sleep(5000);
		rr1.setStop();
		rr2.setStop();
		rr3.setStop();
		rr4.setStop();
		rr5.setStop();
		
		System.out.println("RR1 valid count = " + rr1.getValidCount() + ", invalid count = " + rr1.getInvalidCount());
		assertTrue(rr1.getValidCount() >= rr1.getInvalidCount());
		System.out.println("RR2 valid count = " + rr2.getValidCount() + ", invalid count = " + rr2.getInvalidCount());
		assertTrue(rr2.getValidCount() >= rr2.getInvalidCount());
		System.out.println("RR3 valid count = " + rr3.getValidCount() + ", invalid count = " + rr3.getInvalidCount());
		assertTrue(rr3.getValidCount() >= rr3.getInvalidCount());
		System.out.println("RR4 valid count = " + rr4.getValidCount() + ", invalid count = " + rr4.getInvalidCount());
		assertTrue(rr4.getValidCount() >= rr4.getInvalidCount());
		System.out.println("RR5 valid count = " + rr5.getValidCount() + ", invalid count = " + rr5.getInvalidCount());
		assertTrue(rr5.getValidCount() >= rr5.getInvalidCount());
	}
	
	////////////////////////////////////
	//   Inner class for mocked tests
	////////////////////////////////////
	
	private static class DummyDynamicShardsProvider extends AbstractDynamicShardsProvider<DummyRessource, DummyShardInfo> {
		public DummyDynamicShardsProvider() {
			super();
		}
	}
	
	private static class DummyRessource {
	}
	
	private static class DummyShardInfo extends ShardInfo<DummyRessource> {
		private final String name;
		
		public DummyShardInfo(final String name) {
			super(1);
			this.name = name;
		}
		
		@Override
		protected DummyRessource createResource() {
			return new DummyRessource();
		}

		@Override
		public String getName() {
			return name;
		}
	}
	
	private static class DummySharded extends Sharded<DummyRessource, DummyShardInfo> {
		public DummySharded(AbstractDynamicShardsProvider<DummyRessource, DummyShardInfo> provider) {
			super(provider);
		}
		
	}
	
	private static class ReadRunnable implements Runnable {
		private final List<DummyShardInfo> expected;
		private final DummySharded sharded;
		private boolean stop = false;
		private int validcount = 0;
		private int invalidcount = 0;

		public ReadRunnable(final List<DummyShardInfo> shards , final DummySharded sharded) {
			this.expected = shards;
			this.sharded = sharded;
		}

		public void setStop() {
			this.stop = true;
		}

		public int getValidCount() {
			return validcount;
		}
		public int getInvalidCount() {
			return invalidcount;
		}

		public void run() {
			while(!stop) {
				Collection<DummyShardInfo> result = sharded.getAllShardInfo();
				if(expected.size() * 160 == result.size()) {
					validcount++;
				} else {
					invalidcount++;
				}
				try {
					Thread.currentThread().sleep(200);
				} catch (InterruptedException e) {
					fail(e.getMessage());
				}
			}
		}
	}

	private static class WriteRunnable implements Runnable {
		private final AbstractDynamicShardsProvider<DummyRessource, DummyShardInfo> dynamic;

		public WriteRunnable(final AbstractDynamicShardsProvider<DummyRessource, DummyShardInfo> dynamic) {
			this.dynamic = dynamic;
		}

		public void run() {
			try {
				Thread.currentThread().sleep(4500);
			} catch (InterruptedException e) {
				fail(e.getMessage());
			}
			dynamic.setShards(null);
		}
	}
}

package redis.clients.jedis;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

/**
 * @author secmask@gmail.com
 * 
 */
public class JedisTest {
	@Test
	public void testJedisPoolConnection() throws TimeoutException {
		JedisPool pool = new JedisPool("192.168.65.57", 6379, 1000);
		pool.setResourcesNumber(10);
		pool.setRepairThreadsNumber(1);
		pool.setTimeBetweenCheck(1000);
		pool.init();
		for (int i = 0; i < 1000;) {
			System.out.println("round " + i);
			Jedis con = pool.getResource(Long.MAX_VALUE);
			System.out.println("success obtain resource");
			try {
				con.set("mname" + i, "secmask" + i);
				pool.returnResource(con);
				i++;
			} catch (Exception e) {
				System.out.println("exception: " + e.getMessage());
				pool.returnBrokenResource(con);
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testDynamicPool() throws TimeoutException, InterruptedException {
		JedisDynamicPool pool = new JedisDynamicPool("192.168.2.103", 6379);
		ArrayList<Jedis> coll = new ArrayList<Jedis>();
		for (int i = 0; i < 100; i++) {
			coll.add(pool.getResource());
		}
		for (Jedis j : coll) {
			pool.returnResource(j);
		}
		coll.clear();

		for (int i = 0; i < 1000;) {
			System.out.println("round " + i);
			Jedis con;
			try {
				con = pool.getResource();
				System.out.println("con=" + con);
			} catch (Exception e) {
				e.printStackTrace();
				Thread.sleep(2000);
				continue;
			}
			System.out.println("success obtain resource");
			try {
				con.set("mname" + i, "valuex" + i);
				pool.returnResource(con);
				i++;
			} catch (Exception e) {
				System.out.println("exception: " + e.getMessage());
				pool.returnBrokenResource(con);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testQueue() {
		LinkedBlockingDeque<String> q = new LinkedBlockingDeque<String>();

		long start = System.nanoTime();
		for (int i = 0; i < 1000000000; i++) {
			q.offerFirst("something");
			q.pollFirst();
		}
		System.out.println(System.nanoTime() - start);
	}

	@Test
	public void testSsync() {
		ALinkDeQueue<String> qq = new ALinkDeQueue<String>();
		long start = System.nanoTime();
		for (int i = 0; i < 1000000000; i++) {
			qq.offer("something");
			qq.poll();
		}
		System.out.println(System.nanoTime() - start);
	}

	static class ALinkDeQueue<T> extends AbstractQueue<T> {
		LinkedList<T>	data	= new LinkedList<T>();

		@Override
		public boolean offer(T e) {
			synchronized (data) {
				return data.offerFirst(e);
			}
		}

		@Override
		public T poll() {
			synchronized (data) {
				return data.pollFirst();
			}
		}

		@Override
		public T peek() {
			synchronized (data) {
				return data.peekFirst();
			}
		}

		@Override
		public Iterator<T> iterator() {
			synchronized (data) {
				return data.iterator();
			}
		}

		@Override
		public int size() {
			synchronized (data) {
				return data.size();
			}
		}
	}

}

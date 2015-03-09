package redis.clients.jedis.test;

import java.util.ArrayList;
import java.util.List;

public class MainTest {

	public static void main(String[] args) {
		
		
		List<Integer> list = new ArrayList<Integer>(1000000);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			list.add(i);
		}
		
		System.out.println("Took: " + (System.currentTimeMillis() - start) + " milliseconds");

	}

}

package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ZParams {
    public enum Aggregate {
	SUM, MIN, MAX
    }

    private List<String> params = new ArrayList<String>();

    public ZParams weights(int... weights) {
	params.add("WEIGHTS");
	for (int weight : weights) {
	    params.add(String.valueOf(weight));
	}

	return this;
    }

    public Collection<String> getParams() {
	return Collections.unmodifiableCollection(params);
    }

    public ZParams aggregate(Aggregate aggregate) {
	params.add("AGGREGATE");
	params.add(aggregate.name());
	return this;
    }
}

package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SortingParams {
    private List<String> params = new ArrayList<String>();

    public SortingParams by(String pattern) {
	params.add("BY");
	params.add(pattern);
	return this;
    }

    public Collection<String> getParams() {
	return Collections.unmodifiableCollection(params);
    }

    public SortingParams desc() {
	params.add("DESC");
	return this;
    }

    public SortingParams asc() {
	params.add("ASC");
	return this;
    }

    public SortingParams limit(int start, int count) {
	params.add("LIMIT");
	params.add(String.valueOf(start));
	params.add(String.valueOf(count));
	return this;
    }

    public SortingParams alpha() {
	params.add("ALPHA");
	return this;
    }
}
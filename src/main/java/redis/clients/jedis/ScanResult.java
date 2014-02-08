package redis.clients.jedis;

import java.util.List;

public class ScanResult<T> {
    private long cursor;
    private List<T> results;

    public ScanResult(long cursor, List<T> results) {
	this.cursor = cursor;
	this.results = results;
    }

    public long getCursor() {
	return cursor;
    }

    public List<T> getResult() {
	return results;
    }
}

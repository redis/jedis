package redis.clients.jedis;

import java.util.List;

public class ScanResult<T> {
    private int cursor;
    private List<T> results;

    public ScanResult(int cursor, List<T> results) {
	this.cursor = cursor;
	this.results = results;
    }

    public int getCursor() {
	return cursor;
    }

    public List<T> getResult() {
	return results;
    }
}

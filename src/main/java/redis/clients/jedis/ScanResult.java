package redis.clients.jedis;

import java.util.List;

public class ScanResult<T> {
    private String cursor;
    private List<T> results;

    public ScanResult(String cursor, List<T> results) {
	this.cursor = cursor;
	this.results = results;
    }

    public String getCursor() {
	return cursor;
    }
    
    public List<T> getResult() {
	return results;
    }
}

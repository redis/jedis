package redis.clients.jedis;

public class Jedis extends Client {
    public Jedis(String host) {
	super(host);
    }

    public String ping() {
	return sendCommand("PING").getSingleLineReply();
    }

    public String set(String key, String value) {
	return sendCommand("SET", key, value).getSingleLineReply();

    }

    public String get(String key) {
	return sendCommand("GET", key).getBulkReply();
    }

}

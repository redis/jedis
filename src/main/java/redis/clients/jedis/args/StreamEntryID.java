package redis.clients.jedis.args;

public interface StreamEntryID extends Rawable {

  long milliseconds();

  long sequence();
}

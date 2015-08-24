package redis.clients.jedis;

public interface ConnectionBrokenPattern {
  boolean determine(RuntimeException throwable);
}

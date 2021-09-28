package redis.clients.jedis.commands;

public interface MultiKeyCommands {

  String watch(String... keys);

  String unwatch();

  Long publish(String channel, String message);

}

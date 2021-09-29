package redis.clients.jedis.commands;

//Legacy
public interface MultiKeyCommands {

  String watch(String... keys);

  String unwatch();

  Long publish(String channel, String message);

}

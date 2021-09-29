package redis.clients.jedis.commands;

//Legacy
public interface MultiKeyBinaryCommands {

  String watch(byte[]... keys);

  String unwatch();

  Long publish(byte[] channel, byte[] message);

}

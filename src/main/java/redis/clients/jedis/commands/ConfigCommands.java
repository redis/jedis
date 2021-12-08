package redis.clients.jedis.commands;

import java.util.List;

public interface ConfigCommands {

  List<String> configGet(String pattern);

  List<byte[]> configGet(byte[] pattern);

  String configSet(String parameter, String value);

  String configSet(byte[] parameter, byte[] value);

  String configResetStat();

  String configRewrite();

}

package redis.clients.jedis.util;

import io.redis.test.utils.RedisVersion;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;

import redis.clients.jedis.Module;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.resps.CommandInfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static redis.clients.jedis.Protocol.Command.COMMAND;
import static redis.clients.jedis.Protocol.Command.MODULE;

public class RedisConditions {

  private Map<String, Integer> modules;
  private Map<String, CommandInfo> commands;

  private RedisConditions(Map< String, CommandInfo> commands, Map<String, Integer> modules) {
    this.commands = commands;
    this.modules = modules;
  }

  public static RedisConditions of(UnifiedJedis jedis) {
    RedisVersion version = RedisVersionUtil.getRedisVersion(jedis);

    CommandObject<Map<String, CommandInfo>> commandInfoCmd = new CommandObject<>(new CommandArguments(COMMAND),  CommandInfo.COMMAND_INFO_RESPONSE);
    Map<String, CommandInfo> commands = jedis.executeCommand(commandInfoCmd);

    CommandObject<List<Module>> moduleListCmd = new CommandObject<>(new CommandArguments(MODULE).add(
        Keyword.LIST),  BuilderFactory.MODULE_LIST);

    Map<String, Integer> modules = jedis.executeCommand(moduleListCmd)
        .stream()
        .collect(Collectors.toMap((m)-> m.getName().toUpperCase(), Module::getVersion));

    return new RedisConditions(commands, modules);
  }

  /**
   * @param command
   * @return {@code true} if the command is present.
   */
  public boolean hasCommand(String command) {
    return commands.containsKey(command.toUpperCase());
  }

  /**
   * @param module
   * @return {@code true} if the module is present.
   */
  public boolean hasModule(String module) {
    return modules.containsKey(module.toUpperCase());
  }

  /**
   * @param module
   * @return {@code true} if the module is present.
   */
  public boolean moduleVersionIsGreatherThan(String module, int version) {
    Integer moduleVersion = modules.get(module.toUpperCase());
    return moduleVersion != null && moduleVersion > version;
  }
}

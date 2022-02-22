package redis.clients.jedis.commands;

import redis.clients.jedis.resps.CommandDocs;
import redis.clients.jedis.resps.CommandInfo;
import redis.clients.jedis.resps.KeyedFlags;

import java.util.List;

public interface CommandCommands {

  /**
   * The number of total commands in this Redis server
   * @return The number of total commands
   */
  long commandCount();

  /**
   * Return documentary information about commands.
   * If not specifying commands, the reply includes all the server's commands.
   * @param commands specify the names of one or more commands
   * @return list of {@link CommandDocs}
   */

  List<CommandDocs> commandDocs(String... commands);

  /**
   * Return list of keys from a full Redis command
   * @param command
   * @return list of keys
   */
  List<String> commandGetKeys(String... command);

  /**
   * Return list of keys from a full Redis command and their usage flags
   * @param command
   * @return list of {@link KeyedFlags}
   */
  List<KeyedFlags> commandGetKeysSandFlags(String... command);

  /**
   * Return details about multiple Redis commands
   * @param commands
   * @return list of {@link CommandInfo}
   */
  List<CommandInfo> commandInfo(String... commands);

  /**
   * Return a list of the server's command names
   * @return commands list
   */
  List<String> commandList();

  /**
   * Return a list of the server's command names filter by module's name
   * @param moduleName
   * @return commands list
   */
  List<String> commandListFilterByModule(String moduleName);

  /**
   * Return a list of the server's command names filter by ACL category
   * @param category
   * @return commands list
   */
  List<String> commandListFilterByAclcat(String category);

  /**
   * Return a list of the server's command names filter by glob-like pattern
   * @param pattern
   * @return commands list
   */
  List<String> commandListFilterByPattern(String pattern);
}

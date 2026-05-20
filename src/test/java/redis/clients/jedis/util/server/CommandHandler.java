package redis.clients.jedis.util.server;

import redis.clients.jedis.CommandArguments;

/**
 * Interface for handling custom Redis commands in TcpMockServer. This can be easily mocked with
 * Mockito for testing purposes.
 */
public interface CommandHandler {

  /**
   * Handle a Redis command and return a response.
   * @param commandArgs The command arguments (first element is the command, rest are arguments)
   * @param clientId The client identifier
   * @return A RESP response string, or null to use default handling
   */
  String handleCommand(CommandArguments commandArgs, String clientId);

}

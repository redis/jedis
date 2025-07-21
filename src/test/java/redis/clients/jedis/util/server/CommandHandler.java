package redis.clients.jedis.util.server;

import java.util.List;

/**
 * Interface for handling custom Redis commands in TcpMockServer.
 * This can be easily mocked with Mockito for testing purposes.
 */
public interface CommandHandler {
    
    /**
     * Handle a Redis command and return a response.
     * 
     * @param command The Redis command (case-insensitive)
     * @param args The command arguments (excluding the command name)
     * @param clientId The client identifier
     * @return A RESP response string, or null to use default handling
     */
    String handleCommand(String command, List<String> args, String clientId);
}

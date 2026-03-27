package redis.server.stub.command;

import redis.clients.jedis.CommandArguments;

/**
 * Interface for Redis command implementations.
 * 
 * Design pattern:
 * - Commands validate their own arguments
 * - Commands implement business logic
 * - Registry handles resolution and delegation
 * - Subcommands resolved via resolve() method (like C# RespCommand.Resolve)
 * 
 * Example implementations:
 * - Simple: GetCommand, SetCommand, PingCommand
 * - With subcommands: ClientCommand (SETNAME, GETNAME, ID, etc.)
 */
public interface RedisCommand {
    
    /**
     * Execute the command and return RESP response.
     * Command is responsible for validating its own arguments.
     * 
     * @param args command arguments
     * @param ctx execution context
     * @return RESP-formatted response string
     */
    String execute(CommandArguments args, CommandContext ctx);
    
    /**
     * Command name (e.g., "GET", "SET", "CLIENT").
     * 
     * @return command name in uppercase
     */
    String getName();
    
    /**
     * Check if this command has subcommands.
     * 
     * @return true if command has subcommands (like CLIENT, CONFIG, PUBSUB)
     */
    default boolean hasSubCommands() {
        return false;
    }
    
    /**
     * Resolve subcommand from request (like C# RespCommand.Resolve).
     * Only called if hasSubCommands() returns true.
     * Returns null if subcommand not found.
     * 
     * @param args command arguments (first arg is subcommand name)
     * @return resolved subcommand, or null if not found
     */
    default RedisCommand resolve(CommandArguments args) {
        return null;
    }
}


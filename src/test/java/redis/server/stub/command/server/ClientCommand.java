package redis.server.stub.command.server;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.CommandArguments;
import redis.server.stub.ClientState;
import redis.server.stub.RespResponse;
import redis.server.stub.command.CommandContext;
import redis.server.stub.command.RedisCommand;

/**
 * CLIENT command with subcommands.
 *
 * Subcommands:
 * - SETNAME - Set client name
 * - GETNAME - Get client name
 * - ID - Get client ID
 * - SETINFO - Set client info (LIB-NAME, LIB-VER)
 * - TRACKING - Enable/disable client tracking
 */
public class ClientCommand implements RedisCommand {

    // Internal subcommand registry
    private final Map<String, RedisCommand> subcommands = new HashMap<>();

    public ClientCommand() {
        // Register subcommands internally
        subcommands.put("SETNAME", new ClientSetnameSubCommand());
        subcommands.put("GETNAME", new ClientGetnameSubCommand());
        subcommands.put("ID", new ClientIdSubCommand());
        subcommands.put("SETINFO", new ClientSetinfoSubCommand());
        subcommands.put("TRACKING", new ClientTrackingSubCommand());
    }
    
    @Override
    public String getName() {
        return "CLIENT";
    }
    
    @Override
    public boolean hasSubCommands() {
        return true;
    }
    
    @Override
    public RedisCommand resolve(CommandArguments args) {
        int argCount = args.size() - 1;
        if (argCount < 1) {
            return null; // No subcommand provided
        }
        String subcommand = new String(args.get(1).getRaw(), StandardCharsets.UTF_8).toUpperCase();
        return subcommands.get(subcommand); // Returns null if unknown
    }
    
    @Override
    public String execute(CommandArguments args, CommandContext ctx) {
        // This is called when:
        // 1. CLIENT invoked without arguments (resolve returns null)
        // 2. CLIENT invoked with unknown subcommand (resolve returns null)
        //
        // Redis returns same error for both cases
        return RespResponse.error("ERR wrong number of arguments for 'client' command");
    }
    
    // ===== Nested Subcommand Classes =====
    
    /**
     * CLIENT SETNAME name
     */
    private static class ClientSetnameSubCommand implements RedisCommand {
        @Override
        public String execute(CommandArguments args, CommandContext ctx) {
            int argCount = args.size() - 1;
            if (argCount < 2) {
                return RespResponse.error("ERR wrong number of arguments for 'client|setname' command");
            }

            // args.get(1) = "SETNAME"
            // args.get(2) = name
            String name = new String(args.get(2).getRaw(), StandardCharsets.UTF_8);
            ctx.getClient().setName(name);
            return RespResponse.ok();
        }

        @Override
        public String getName() {
            return "SETNAME";
        }
    }
    
    /**
     * CLIENT GETNAME
     */
    private static class ClientGetnameSubCommand implements RedisCommand {
        @Override
        public String execute(CommandArguments args, CommandContext ctx) {
            String name = ctx.getClient().getName();
            return RespResponse.bulkString(name); // Handles null automatically
        }

        @Override
        public String getName() {
            return "GETNAME";
        }
    }
    
    /**
     * CLIENT ID
     */
    private static class ClientIdSubCommand implements RedisCommand {
        @Override
        public String execute(CommandArguments args, CommandContext ctx) {
            long id = ctx.getClient().getId();
            return RespResponse.integer(id);
        }

        @Override
        public String getName() {
            return "ID";
        }
    }
    
    /**
     * CLIENT SETINFO LIB-NAME libname | LIB-VER libver
     */
    private static class ClientSetinfoSubCommand implements RedisCommand {
        @Override
        public String execute(CommandArguments args, CommandContext ctx) {
            int argCount = args.size() - 1;
            if (argCount < 3) {
                return RespResponse.error("ERR wrong number of arguments for 'client|setinfo' command");
            }

            // args.get(1) = "SETINFO"
            // args.get(2) = attribute (LIB-NAME or LIB-VER)
            // args.get(3) = value
            String attribute = new String(args.get(2).getRaw(), StandardCharsets.UTF_8).toUpperCase();
            String value = new String(args.get(3).getRaw(), StandardCharsets.UTF_8);

            ClientState client = ctx.getClient();
            switch (attribute) {
                case "LIB-NAME":
                    client.setLibName(value);
                    break;
                case "LIB-VER":
                    client.setLibVersion(value);
                    break;
                default:
                    return RespResponse.error("ERR unknown attribute '" + attribute + "'");
            }

            return RespResponse.ok();
        }

        @Override
        public String getName() {
            return "SETINFO";
        }
    }

    /**
     * CLIENT TRACKING ON|OFF [REDIRECT client-id] [PREFIX prefix] [BCAST] [OPTIN] [OPTOUT] [NOLOOP]
     *
     * For testing purposes, we only handle ON/OFF and ignore optional parameters.
     * This is sufficient for CacheConnection.initializeClientSideCache().
     */
    private static class ClientTrackingSubCommand implements RedisCommand {
        @Override
        public String execute(CommandArguments args, CommandContext ctx) {
            int argCount = args.size() - 1;
            if (argCount < 2) {
                return RespResponse.error("ERR wrong number of arguments for 'client|tracking' command");
            }

            // args.get(1) = "TRACKING"
            // args.get(2) = ON or OFF
            String onOff = new String(args.get(2).getRaw(), StandardCharsets.UTF_8).toUpperCase();

            boolean enable;
            switch (onOff) {
                case "ON":
                    enable = true;
                    break;
                case "OFF":
                    enable = false;
                    break;
                default:
                    return RespResponse.error("ERR CLIENT TRACKING requires ON or OFF");
            }

            // Set tracking state on client
            ctx.getClient().setTrackingEnabled(enable);

            // Optional parameters (REDIRECT, PREFIX, BCAST, etc.) are ignored for now
            // This is sufficient for testing CacheConnection initialization

            return RespResponse.ok();
        }

        @Override
        public String getName() {
            return "TRACKING";
        }
    }
}


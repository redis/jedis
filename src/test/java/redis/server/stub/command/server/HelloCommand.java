package redis.server.stub.command.server;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.util.SafeEncoder;
import redis.server.stub.RespResponse;
import redis.server.stub.command.RedisCommand;
import redis.server.stub.command.CommandContext;

/**
 * HELLO [protover] [AUTH username password] [SETNAME clientname]
 * <p>
 * RESP3 protocol handshake command. Returns server information including: - server: redis -
 * version: 7.0.0 - proto: protocol version (always 3) - id: client ID - mode: standalone - role:
 * master - modules: loaded modules
 * <p>
 * <b>Protocol Support</b>: This implementation only supports RESP3 (protocol version 3). Any
 * attempt to negotiate RESP2 or other protocols will be rejected with NOPROTO error.
 */
public class HelloCommand implements RedisCommand {

  @Override
  public String execute(CommandArguments args, CommandContext ctx) {
    int argCount = args.size() - 1;

    // Default to RESP3
    int requestedProtocol = 3;

    // Parse protocol version if provided
    if (argCount >= 1) {
      try {
        String protoStr = SafeEncoder.encode(args.get(1).getRaw());
        requestedProtocol = Integer.parseInt(protoStr);
      } catch (NumberFormatException e) {
        return RespResponse.error("ERR Protocol version is not an integer or out of range");
      }
    }

    // Validate RESP3 only
    if (requestedProtocol != 3) {
      return RespResponse.error(
        "NOPROTO unsupported protocol version. This server only supports RESP3 (protocol version 3)");
    }

    // Process optional AUTH and SETNAME
    for (int i = 2; i < args.size(); i++) {
      String arg = SafeEncoder.encode(args.get(i).getRaw()).toUpperCase();
      if ("AUTH".equals(arg) && i + 2 < args.size()) {
        // Simple authentication - accept any credentials for testing
        ctx.getClient().setAuthenticated(true);
        i += 2; // Skip username and password
      } else if ("SETNAME".equals(arg) && i + 1 < args.size()) {
        String name = SafeEncoder.encode(args.get(i + 1).getRaw());
        ctx.getClient().setName(name);
        i += 1;
      }
    }

    // Protocol version is already 3 (fixed in ClientState)
    return buildHelloResponse(3, ctx);
  }

  @Override
  public String getName() {
    return "HELLO";
  }

  /**
   * Build HELLO response as RESP3 map using RespResponse primitives. Format:
   * %<count>\r\n<key1><value1><key2><value2>...
   * @param protocol protocol version (always 3 for this implementation)
   * @param ctx command context (to get server config)
   * @return RESP3 map response
   */
  private String buildHelloResponse(int protocol, CommandContext ctx) {
    // Get Redis version from server config
    String version = ctx.getServer().getConfig().getRedisVersion();

    // Build map entries: key-value pairs
    return RespResponse.map(RespResponse.bulkString("server"), RespResponse.bulkString("redis"),
      RespResponse.bulkString("version"), RespResponse.bulkString(version),
      RespResponse.bulkString("proto"), RespResponse.integer(protocol),
      RespResponse.bulkString("id"), RespResponse.integer(ctx.getClient().getId()),
      RespResponse.bulkString("mode"), RespResponse.bulkString("standalone"),
      RespResponse.bulkString("role"), RespResponse.bulkString("master"),
      RespResponse.bulkString("modules"), RespResponse.emptyArray());
  }
}

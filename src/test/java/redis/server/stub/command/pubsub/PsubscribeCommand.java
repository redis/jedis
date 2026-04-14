package redis.server.stub.command.pubsub;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.util.SafeEncoder;
import redis.server.stub.PubSubManager;
import redis.server.stub.RespResponse;
import redis.server.stub.Subscriber;
import redis.server.stub.command.CommandContext;
import redis.server.stub.command.RedisCommand;

/**
 * PSUBSCRIBE pattern [pattern ...]
 * <p>
 * Subscribe to one or more patterns. Supports glob-style patterns:
 * <ul>
 * <li>{@code *} - matches zero or more characters</li>
 * <li>{@code ?} - matches exactly one character</li>
 * <li>{@code [abc]} - matches one character from the set</li>
 * </ul>
 * <p>
 * For each pattern, sends a subscription confirmation:
 * 
 * <pre>
 * *3
 * $10
 * psubscribe
 * $6
 * news.*
 * :1     &lt;- Total subscription count for this client (channels + patterns)
 * </pre>
 * <p>
 * When a message is published to a channel matching the pattern, the client receives a "pmessage"
 * push with the pattern, channel, and message.
 */
public class PsubscribeCommand implements RedisCommand {

  @Override
  public String execute(CommandArguments args, CommandContext ctx) {
    // Validate: PSUBSCRIBE requires at least one pattern
    if (args.size() < 2) {
      return RespResponse.error("ERR wrong number of arguments for 'psubscribe' command");
    }

    PubSubManager pubSubManager = ctx.getServer().getPubSubManager();
    Subscriber subscriber = ctx.getSubscriber();

    StringBuilder response = new StringBuilder();

    // Subscribe to each pattern
    for (int i = 1; i < args.size(); i++) {
      String pattern = SafeEncoder.encode(args.get(i).getRaw());

      // Update subscription state (both maps in PubSubManager)
      pubSubManager.psubscribe(subscriber, pattern);

      // Get current total subscription count (channels + patterns)
      int count = pubSubManager.getSubscriptionCount(subscriber);

      // Build RESP3 array response: *3 $10 psubscribe $... pattern :count
      response.append(RespResponse.array(RespResponse.bulkString("psubscribe"),
        RespResponse.bulkString(pattern), RespResponse.integer(count)));
    }

    return response.toString();
  }

  @Override
  public String getName() {
    return "PSUBSCRIBE";
  }
}

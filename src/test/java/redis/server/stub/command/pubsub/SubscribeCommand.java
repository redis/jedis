package redis.server.stub.command.pubsub;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.util.SafeEncoder;
import redis.server.stub.PubSubManager;
import redis.server.stub.RespResponse;
import redis.server.stub.Subscriber;
import redis.server.stub.command.CommandContext;
import redis.server.stub.command.RedisCommand;

/**
 * SUBSCRIBE channel [channel ...]
 * <p>
 * Subscribe to one or more channels. For each channel, sends a subscription confirmation:
 * 
 * <pre>
 * *3
 * $9
 * subscribe
 * $7
 * channel
 * :1     &lt;- Total subscription count for this subscriber
 * </pre>
 * <p>
 * After subscribing, the subscriber enters pub/sub mode and will receive push messages when
 * messages are published to subscribed channels.
 */
public class SubscribeCommand implements RedisCommand {

  @Override
  public String execute(CommandArguments args, CommandContext ctx) {
    // Validate: SUBSCRIBE requires at least one channel
    if (args.size() < 2) {
      return RespResponse.error("ERR wrong number of arguments for 'subscribe' command");
    }

    PubSubManager pubSubManager = ctx.getServer().getPubSubManager();
    Subscriber subscriber = ctx.getSubscriber();

    StringBuilder response = new StringBuilder();

    // Subscribe to each channel
    for (int i = 1; i < args.size(); i++) {
      String channel = SafeEncoder.encode(args.get(i).getRaw());

      // Update subscription state (both maps in PubSubManager)
      pubSubManager.subscribe(subscriber, channel);

      // Get current total subscription count
      int count = pubSubManager.getSubscriptionCount(subscriber);

      // Build RESP3 array response: *3 $9 subscribe $... channel :count
      response.append(RespResponse.array(RespResponse.bulkString("subscribe"),
        RespResponse.bulkString(channel), RespResponse.integer(count)));
    }

    return response.toString();
  }

  @Override
  public String getName() {
    return "SUBSCRIBE";
  }
}

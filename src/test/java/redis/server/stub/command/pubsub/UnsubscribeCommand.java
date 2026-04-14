package redis.server.stub.command.pubsub;

import java.util.HashSet;
import java.util.Set;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.util.SafeEncoder;
import redis.server.stub.PubSubManager;
import redis.server.stub.RespResponse;
import redis.server.stub.Subscriber;
import redis.server.stub.CommandContext;
import redis.server.stub.RedisCommand;

/**
 * UNSUBSCRIBE [channel [channel ...]]
 * <p>
 * Unsubscribe from one or more channels. If no channels specified, unsubscribes from ALL channels.
 * <p>
 * For each channel, sends an unsubscribe confirmation:
 * 
 * <pre>
 * *3
 * $11
 * unsubscribe
 * $7
 * channel
 * :0     &lt;- Remaining subscription count for this client
 * </pre>
 */
public class UnsubscribeCommand implements RedisCommand {

  @Override
  public String execute(CommandArguments args, CommandContext ctx) {
    PubSubManager pubSubManager = ctx.getServer().getPubSubManager();
    Subscriber subscriber = ctx.getSubscriber();

    StringBuilder response = new StringBuilder();

    if (args.size() == 1) {
      // UNSUBSCRIBE with no args -> unsubscribe from ALL channels
      Set<String> channels = pubSubManager.getSubscriberChannels(subscriber);

      if (channels.isEmpty()) {
        // No channels to unsubscribe from
        response.append(RespResponse.array(RespResponse.bulkString("unsubscribe"),
          RespResponse.nullBulkString(), RespResponse.integer(0)));
      } else {
        // Unsubscribe from all channels
        // Copy to avoid concurrent modification
        for (String channel : new HashSet<>(channels)) {
          pubSubManager.unsubscribe(subscriber, channel);
          int count = pubSubManager.getSubscriptionCount(subscriber);

          response.append(RespResponse.array(RespResponse.bulkString("unsubscribe"),
            RespResponse.bulkString(channel), RespResponse.integer(count)));
        }
      }
    } else {
      // UNSUBSCRIBE from specific channels
      for (int i = 1; i < args.size(); i++) {
        String channel = SafeEncoder.encode(args.get(i).getRaw());
        pubSubManager.unsubscribe(subscriber, channel);
        int count = pubSubManager.getSubscriptionCount(subscriber);

        response.append(RespResponse.array(RespResponse.bulkString("unsubscribe"),
          RespResponse.bulkString(channel), RespResponse.integer(count)));
      }
    }

    return response.toString();
  }

  @Override
  public String getName() {
    return "UNSUBSCRIBE";
  }
}

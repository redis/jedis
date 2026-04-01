package redis.server.stub.command.pubsub;

import java.util.HashSet;
import java.util.Set;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.util.SafeEncoder;
import redis.server.stub.PubSubManager;
import redis.server.stub.RespResponse;
import redis.server.stub.Subscriber;
import redis.server.stub.command.CommandContext;
import redis.server.stub.command.RedisCommand;

/**
 * PUNSUBSCRIBE [pattern [pattern ...]]
 * 
 * <p>Unsubscribe from one or more patterns. If no patterns specified, unsubscribes from ALL
 * patterns.
 * 
 * <p>For each pattern, sends an unsubscribe confirmation:
 * <pre>
 * *3
 * $12
 * punsubscribe
 * $6
 * news.*
 * :0     &lt;- Remaining subscription count for this client
 * </pre>
 */
public class PunsubscribeCommand implements RedisCommand {

  @Override
  public String execute(CommandArguments args, CommandContext ctx) {
    PubSubManager pubSubManager = ctx.getServer().getPubSubManager();
    Subscriber subscriber = ctx.getSubscriber();

    StringBuilder response = new StringBuilder();

    if (args.size() == 1) {
      // PUNSUBSCRIBE with no args -> unsubscribe from ALL patterns
      Set<String> patterns = pubSubManager.getSubscriberPatterns(subscriber);

      if (patterns.isEmpty()) {
        // No patterns to unsubscribe from
        response.append(RespResponse.array(RespResponse.bulkString("punsubscribe"),
            RespResponse.nullBulkString(), RespResponse.integer(0)));
      } else {
        // Unsubscribe from all patterns
        // Copy to avoid concurrent modification
        for (String pattern : new HashSet<>(patterns)) {
          pubSubManager.punsubscribe(subscriber, pattern);
          int count = pubSubManager.getSubscriptionCount(subscriber);

          response.append(RespResponse.array(RespResponse.bulkString("punsubscribe"),
              RespResponse.bulkString(pattern), RespResponse.integer(count)));
        }
      }
    } else {
      // PUNSUBSCRIBE from specific patterns
      for (int i = 1; i < args.size(); i++) {
        String pattern = SafeEncoder.encode(args.get(i).getRaw());
        pubSubManager.punsubscribe(subscriber, pattern);
        int count = pubSubManager.getSubscriptionCount(subscriber);

        response.append(RespResponse.array(RespResponse.bulkString("punsubscribe"),
            RespResponse.bulkString(pattern), RespResponse.integer(count)));
      }
    }

    return response.toString();
  }

  @Override
  public String getName() {
    return "PUNSUBSCRIBE";
  }
}


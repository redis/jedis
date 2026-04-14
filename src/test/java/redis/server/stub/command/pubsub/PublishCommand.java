package redis.server.stub.command.pubsub;

import java.util.Set;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.util.SafeEncoder;
import redis.server.stub.PubSubManager;
import redis.server.stub.RespResponse;
import redis.server.stub.Subscriber;
import redis.server.stub.CommandContext;
import redis.server.stub.RedisCommand;

/**
 * PUBLISH channel message
 * <p>
 * Publish a message to a channel. All clients subscribed to the channel (exact or via matching
 * patterns) receive the message as a push notification.
 * <p>
 * Response: Integer indicating number of clients that received the message:
 * 
 * <pre>
 * :2     &lt;- Number of subscribers
 * </pre>
 * <p>
 * Subscribers receive messages as RESP3 push:
 * <ul>
 * <li>Exact subscribers: {@code >3 $7 message $... channel $... message}</li>
 * <li>Pattern subscribers: {@code >4 $8 pmessage $... pattern $... channel $... message}</li>
 * </ul>
 * <p>
 * Note: If a client is subscribed both exactly and via pattern, it receives only ONE message (the
 * exact subscription takes priority to avoid duplicates).
 */
public class PublishCommand implements RedisCommand {

  @Override
  public String execute(CommandArguments args, CommandContext ctx) {
    // Validate: PUBLISH requires exactly 2 arguments (channel and message)
    if (args.size() != 3) {
      return RespResponse.error("ERR wrong number of arguments for 'publish' command");
    }

    String channel = SafeEncoder.encode(args.get(1).getRaw());
    String message = SafeEncoder.encode(args.get(2).getRaw());

    PubSubManager pubSubManager = ctx.getServer().getPubSubManager();

    // Publish message to all matching subscribers
    // PubSubManager.publish() sends push messages and returns recipients
    Set<Subscriber> recipients = pubSubManager.publish(channel, message);

    // Return number of subscribers that received the message
    return RespResponse.integer(recipients.size());
  }

  @Override
  public String getName() {
    return "PUBLISH";
  }
}

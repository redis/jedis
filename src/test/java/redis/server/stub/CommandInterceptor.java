package redis.server.stub;

import redis.clients.jedis.CommandArguments;

/**
 * Interceptor for command execution in RedisServerStub. Allows test code to verify state and inject
 * push messages BEFORE command executes.
 * <p>
 * Key characteristics:
 * <ul>
 * <li>Runs on single-threaded command executor (thread-safe)</li>
 * <li>Executes BEFORE command logic (non-invasive)</li>
 * <li>Can inspect arguments and client state</li>
 * <li>Can inject push messages, modify state, make assertions</li>
 * <li>All exceptions propagate to test (fail fast)</li>
 * </ul>
 * <p>
 * Usage example:
 *
 * <pre>
 * {@code
 * server.beforeCommand("PUBLISH", (args, ctx) -> {
 *     // Verify subscription state
 *     assertEquals(1, ctx.getClient().getSubscriptionCount());
 *
 *     // Inject push message
 *     server.injectPushMessage(ctx.getClient(), "MOVING", "1", "15", "new-host:6380");
 * });
 * }
 * </pre>
 *
 * @see CommandContext
 */
@FunctionalInterface
public interface CommandInterceptor {

  /**
   * Called before command execution. Runs on single-threaded command executor.
   * @param args command arguments (command name + args)
   * @param ctx execution context (access to client, data store, server)
   * @throws Exception any exception propagates to test (fails test)
   */
  void beforeExecute(CommandArguments args, CommandContext ctx) throws Exception;

}

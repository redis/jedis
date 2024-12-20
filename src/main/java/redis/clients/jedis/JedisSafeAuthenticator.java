package redis.clients.jedis;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.Token;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.authentication.JedisAuthenticationException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

class JedisSafeAuthenticator {

  private static final Token PLACEHOLDER_TOKEN = new SimpleToken(null, null, 0, 0, null);
  private static final Logger logger = LoggerFactory.getLogger(JedisSafeAuthenticator.class);

  protected volatile Connection client;
  protected final Consumer<Object> authResultHandler = this::processAuthReply;
  protected final Consumer<Token> authenticationHandler = this::safeReAuthenticate;

  protected final AtomicReference<Token> pendingTokenRef = new AtomicReference<Token>(null);
  protected final ReentrantLock commandSync = new ReentrantLock();
  protected final Queue<Consumer<Object>> resultHandler = new ConcurrentLinkedQueue<Consumer<Object>>();

  protected void sendAndFlushCommand(Command command, Object... args) {
    if (client == null) {
      throw new JedisException(getClass() + " is not connected to a Connection.");
    }
    CommandArguments cargs = new CommandArguments(command).addObjects(args);

    Token newToken = pendingTokenRef.getAndSet(PLACEHOLDER_TOKEN);

    // lets send the command without locking !!IF!! we know that pendingTokenRef is null replaced with PLACEHOLDER_TOKEN and no re-auth will go into action
    // !!ELSE!! we are locking since we already know a re-auth is still in progress in another thread and we need to wait for it to complete, we do nothing but wait on it!
    if (newToken != null) {
      commandSync.lock();
    }
    try {
      client.sendCommand(cargs);
      client.flush();
    } finally {
      Token newerToken = pendingTokenRef.getAndSet(null);
      // lets check if a newer token received since the beginning of this sendAndFlushCommand call
      if (newerToken != null && newerToken != PLACEHOLDER_TOKEN) {
        safeReAuthenticate(newerToken);
      }
      if (newToken != null) {
        commandSync.unlock();
      }
    }
  }

  protected void registerForAuthentication(Connection newClient) {
    Connection oldClient = this.client;
    if (oldClient == newClient) return;
    if (oldClient != null && oldClient.getAuthXManager() != null) {
      oldClient.getAuthXManager().removePostAuthenticationHook(authenticationHandler);
    }
    if (newClient != null && newClient.getAuthXManager() != null) {
      newClient.getAuthXManager().addPostAuthenticationHook(authenticationHandler);
    }
    this.client = newClient;
  }

  private void safeReAuthenticate(Token token) {
    try {
      byte[] rawPass = client.encodeToBytes(token.getValue().toCharArray());
      byte[] rawUser = client.encodeToBytes(token.getUser().toCharArray());

      Token newToken = pendingTokenRef.getAndSet(token);
      if (newToken == null) {
        commandSync.lock();
        try {
          sendAndFlushCommand(Command.AUTH, rawUser, rawPass);
          resultHandler.add(this.authResultHandler);
        } finally {
          pendingTokenRef.set(null);
          commandSync.unlock();
        }
      }
    } catch (Exception e) {
      logger.error("Error while re-authenticating connection", e);
      client.getAuthXManager().getListener().onConnectionAuthenticationError(e);
    }
  }

  protected void processAuthReply(Object reply) {
    byte[] resp = (byte[]) reply;
    String response = SafeEncoder.encode(resp);
    if (!"OK".equals(response)) {
      String msg = "Re-authentication failed with server response: " + response;
      Exception failedAuth = new JedisAuthenticationException(msg);
      logger.error(failedAuth.getMessage(), failedAuth);
      client.getAuthXManager().getListener().onConnectionAuthenticationError(failedAuth);
    }
  }
}

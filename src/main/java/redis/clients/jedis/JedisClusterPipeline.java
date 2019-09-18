package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.util.JedisClusterCRC16;

/**
 * This implementation is based on the facts that class "redis.clients.jedis.PipelineBase" is
 * implemented in this way.
 * 
 * <pre>
 * public Response<String> get(String key) {
 *   getClient(key).get(key);
 *   return getResponse(BuilderFactory.STRING);
 * }
 * </pre>
 * 
 * All operations defined in "PipelineBase" will call "getClient" and then "getResponse". So we keep
 * states in class members and share among these methods. The operation is recorded and will be
 * replayed when retrying.
 * <p>
 * Adding cluster pipelining support need just small changes on other classes.
 */
public class JedisClusterPipeline extends PipelineBase implements CommandListener {

  private static final int MAX_PIPELINE_RETRIES = 20;

  private JedisClusterConnectionHandler connectionHandler;
  private Map<HostAndPort, Jedis> connectionCacheByNode = new HashMap<HostAndPort, Jedis>();

  private List<PipelineCommand> cmds = new ArrayList<PipelineCommand>();
  private List<PipelineCommand> responses = Collections.emptyList();

  private boolean isRecording = true;
  private PipelineCommand lastCommand = null;
  private Jedis lastConnection = null;
  private HostAndPort lastSlotNode = null;
  private int lastSlot = -1;

  private int counterOfAsking = 0;
  private int counterOfMoving = 0;

  public int getCounterOfAsking() {
    return counterOfAsking;
  }

  public int getCounterOfMoving() {
    return counterOfMoving;
  }

  public JedisClusterPipeline(JedisClusterConnectionHandler connectionHandler) {
    this.connectionHandler = connectionHandler;
  }

  @Override
  public void afterCommand(Connection conn, ProtocolCommand cmd, byte[][] args) {
    if (isRecording) {
      if (lastConnection.getClient() != conn) {
        throw new JedisClusterException("Pipeline state error");
      }

      // Cache commands sent, re-send them if needed.
      PipelineCommand operation = new PipelineCommand();
      operation.setConnection(lastConnection);
      operation.setClient((Client) conn);
      operation.setNode(lastSlotNode);
      operation.setCommand(cmd);
      operation.setArgs(args);

      // Use the slot in last call on getClient, because every operation requires a client
      // for example: getClient(key).decr(key);
      operation.setSlot(lastSlot);
      lastCommand = operation;
    }
  }

  @Override
  protected Client getClient(String key) {
    if (lastConnection != null) {
      throw new JedisClusterException("Pipeline state error");
    }

    int slot = JedisClusterCRC16.getSlot(key);
    HostAndPort node = connectionHandler.getSlotNode(slot);
    Jedis connection = connectionCacheByNode.get(node);
    if (connection == null) {
      connection = connectionHandler.getConnectionFromNode(node);
      connectionCacheByNode.put(node, connection);
    }

    lastConnection = connection;
    lastSlotNode = node;
    lastSlot = slot;

    Client result = connection.getClient();
    result.setCommandListener(this);
    return result;
  }

  @Override
  protected Client getClient(byte[] key) {
    if (lastConnection != null) {
      throw new JedisClusterException("Pipeline state error");
    }

    int slot = JedisClusterCRC16.getSlot(key);
    HostAndPort node = connectionHandler.getSlotNode(slot);
    Jedis connection = connectionCacheByNode.get(node);
    if (connection == null) {
      connection = connectionHandler.getConnectionFromNode(node);
      connectionCacheByNode.put(node, connection);
    }

    lastConnection = connection;
    lastSlotNode = node;
    lastSlot = slot;

    Client result = connection.getClient();
    result.setCommandListener(this);
    return result;
  }

  private Jedis getConnection(HostAndPort node) {
    Jedis connection = connectionCacheByNode.get(node);
    if (connection == null) {
      connection = connectionHandler.getConnectionFromNode(node);
      connectionCacheByNode.put(node, connection);
    }

    Client result = connection.getClient();
    result.setCommandListener(this);
    return connection;
  }

  @Override
  protected void clean() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Response<?> generateResponse(Object data) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected <T> Response<T> getResponse(Builder<T> builder) {
    Response<T> result = new Response<T>(builder);

    if (isRecording && lastCommand != null) {
      PipelineCommand cmd = lastCommand;
      cmd.setResp(result);
      cmds.add(cmd);

      lastCommand = null;
      lastConnection = null;
      lastSlotNode = null;
      lastSlot = -1;

      return result;
    }

    throw new IllegalStateException("Must be called after sendCommand");
  }

  @Override
  protected boolean hasPipelinedResponse() {
    return !cmds.isEmpty();
  }

  @Override
  protected int getPipelinedResponseLength() {
    return cmds.size();
  }

  protected void syncImpl() {
    List<PipelineCommand> retries = new ArrayList<>(cmds.size());
    List<PipelineCommand> operations = new ArrayList<>(cmds.size());

    operations.addAll(cmds);

    int retriedTimes = 0;
    while (retriedTimes < MAX_PIPELINE_RETRIES) {
      retriedTimes++;

      // All commands have been sent
      for (PipelineCommand operation : operations) {
        Client client = operation.getClient();

        try {
          if (operation.isAsking()) {
            // Read response of asking, should be always "OK"
            client.getOne();
          }

          // Reset asking flag
          operation.setAsking(false);

          // Read responses for each command
          Object data = client.getOne();
          operation.getResp().set(data);
        } catch (JedisMovedDataException e) {
          counterOfMoving++;

          // if moved message received, update slots mapping
          int slot = e.getSlot();
          HostAndPort node = e.getTargetNode();

          Jedis connection = getConnection(node);
          operation.setClient(connection.getClient());
          operation.setConnection(connection);
          operation.setNode(node);

          // update slot-node mapping
          connectionHandler.assignSlotToNode(slot, node);

          // Will retry later
          retries.add(operation);
        } catch (JedisAskDataException e) {
          counterOfAsking++;

          // if asked message received, send asking before next retrying,
          // but do not cache and update slots mapping
          operation.setAsking(true);
          HostAndPort node = e.getTargetNode();

          Jedis connection = getConnection(node);
          operation.setClient(connection.getClient());
          operation.setConnection(connection);
          operation.setNode(node);

          // Will retry later
          retries.add(operation);
        } catch (JedisException e) {
          operation.setError(e);
          break;
        }
      }

      // All commands are completed
      if (retries.isEmpty()) {
        break;
      }

      // Re-send commands for redirection
      for (PipelineCommand operation : retries) {
        Client client = operation.getClient();
        if (operation.isAsking()) {
          client.asking();
        }

        ProtocolCommand cmd = operation.getCommand();
        byte[][] args = operation.getArgs();
        client.sendCommand(cmd, args);
      }

      operations.clear();
      operations.addAll(retries);
      retries.clear();
    }
  }

  private void releaseConnection(Jedis connection) {
    if (connection != null) {
      Client client = connection.getClient();
      client.clearCommandListener();
      connection.close();
    }
  }

  /**
   * Release all used connections.
   */
  private void reset() {
    Collection<Jedis> connectionList = connectionCacheByNode.values();
    for (Jedis connection : connectionList) {
      releaseConnection(connection);
    }

    cmds = new ArrayList<PipelineCommand>();
    connectionCacheByNode.clear();
    lastCommand = null;
    lastConnection = null;
    lastSlot = -1;
  }

  /**
   * Synchronise pipeline by reading all responses. This operation closes the pipeline. In order to
   * get return values from pipelined commands, capture the different Response&lt;?&gt; of the
   * commands you execute.
   */
  public void sync() {
    try {
      isRecording = false;
      counterOfMoving = 0;
      counterOfAsking = 0;

      syncImpl();
    } finally {
      // Pipeline could be used more than once.
      responses = cmds;
      isRecording = true;
      reset();
    }
  }

  public List<Object> getResults() {
    List<Object> result = new ArrayList<Object>();
    if (responses != null) {
      for (PipelineCommand cmd : responses) {
        result.add(cmd.getResp().get());
      }
    }

    return result;
  }

  public List<Object> syncAndReturnAll() {
    sync();
    return getResults();
  }
}

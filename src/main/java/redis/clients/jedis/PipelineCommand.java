package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisException;

public class PipelineCommand {

  private Jedis connection;

  private Client client;

  private Response<?> resp;

  private ProtocolCommand cmd;

  private byte[][] args;

  private HostAndPort node;

  private boolean asking = false;

  private int slot = -1;

  private JedisException error;

  public Jedis getConnection() {
    return connection;
  }

  public void setConnection(Jedis connection) {
    this.connection = connection;
  }

  public Client getClient() {
    return client;
  }

  public void setClient(Client client) {
    this.client = client;
  }

  public Response<?> getResp() {
    return resp;
  }

  public void setResp(Response<?> resp) {
    this.resp = resp;
  }

  public ProtocolCommand getCommand() {
    return cmd;
  }

  public void setCommand(ProtocolCommand cmd) {
    this.cmd = cmd;
  }

  public HostAndPort getNode() {
    return node;
  }

  public void setNode(HostAndPort node) {
    this.node = node;
  }

  public byte[][] getArgs() {
    return args;
  }

  public void setArgs(byte[][] args) {
    this.args = args;
  }

  public boolean isAsking() {
    return asking;
  }

  public void setAsking(boolean asking) {
    this.asking = asking;
  }

  public int getSlot() {
    return slot;
  }

  public void setSlot(int slot) {
    this.slot = slot;
  }

  public JedisException getError() {
    return error;
  }

  public void setError(JedisException error) {
    this.error = error;
  }

}
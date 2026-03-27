package redis.server.stub;

class CommandContextImpl implements CommandContext {

  private final ClientState client;

  private final RedisDataStore dataStore;

  private final RedisServerStub server;

  CommandContextImpl(ClientState client, RedisDataStore dataStore, RedisServerStub server) {
    this.client = client;
    this.dataStore = dataStore;
    this.server = server;
  }

  @Override
  public ClientState getClient() {
    return client;
  }

  @Override
  public RedisDataStore getDataStore() {
    return dataStore;
  }

  @Override
  public RedisServerStub getServer() {
    return server;
  }

}

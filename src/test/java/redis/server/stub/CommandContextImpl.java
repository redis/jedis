package redis.server.stub;

class CommandContextImpl implements CommandContext {

  private final ClientState client;

  private final RedisDataStore dataStore;

  private final RedisServerStub server;

  private final Subscriber subscriber;

  CommandContextImpl(ClientState client, RedisDataStore dataStore, RedisServerStub server,
      Subscriber subscriber) {
    this.client = client;
    this.dataStore = dataStore;
    this.server = server;
    this.subscriber = subscriber;
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

  @Override
  public Subscriber getSubscriber() {
    return subscriber;
  }

}

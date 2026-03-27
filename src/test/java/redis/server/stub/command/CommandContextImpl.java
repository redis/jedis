package redis.server.stub.command;

import redis.server.stub.ClientState;
import redis.server.stub.RedisDataStore;
import redis.server.stub.RedisServerStub;

/**
 * Implementation of CommandContext.
 * Simple immutable value holder.
 */
public class CommandContextImpl implements CommandContext {
    
    private final ClientState client;
    private final RedisDataStore dataStore;
    private final RedisServerStub server;

    public CommandContextImpl(ClientState client,
                             RedisDataStore dataStore,
                             RedisServerStub server) {
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


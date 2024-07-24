package redis.clients.jedis.csc;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;

public interface DataProvider {

    public Connection getSource();

    public <T> T getData(CommandObject<T> commandObject);
    
    public void consumeInvalidationMessages();
}

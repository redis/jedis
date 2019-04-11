package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;

class NoopJedisCommandListener implements JedisCommandListener {
    public static final JedisCommandListener INSTANCE = new NoopJedisCommandListener();

    private NoopJedisCommandListener() {
    }

    @Override
    public void commandStarted(Connection connection, ProtocolCommand event, byte[]... args) {
    }

    @Override
    public void commandFinished(Connection connection, ProtocolCommand event) {
    }

    @Override
    public void commandFailed(Connection connection, ProtocolCommand event, Throwable t) {
    }
}

package redis.clients.jedis.commands;

import java.util.List;

public interface RedisCommandCommands {

    public List<String> command(); // todo: need to fix returned value and test

    public Long commandCount();

    public List<String> commandGetKeys(String... command);
}

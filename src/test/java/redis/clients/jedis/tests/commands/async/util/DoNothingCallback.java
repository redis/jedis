package redis.clients.jedis.tests.commands.async.util;

public static class DoNothingCallback<T> implements AsyncResponseCallback<T> {
        @Override
        public void execute(T response, JedisException exc) {
            // do nothing
        }
    }


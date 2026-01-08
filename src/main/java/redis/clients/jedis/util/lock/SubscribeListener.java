package redis.clients.jedis.util.lock;

import redis.clients.jedis.JedisPubSub;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

public class SubscribeListener extends JedisPubSub {
    private Set<Thread> subscribers;

    protected SubscribeListener(Set<Thread> subscribers) {
        this.subscribers = subscribers;
    }

    @Override
    public void onMessage(String channel, String message) {
        synchronized (subscribers) {
            for (Thread subscriber : subscribers) {
                LockSupport.unpark(subscriber);
            }
        }
    }
}

package redis.clients.jedis;

/**
 * Callback for using an async response. Keep in mind that the callback will
 * come back on an unreliable thread (possibly the caller thread).
 */
public interface ResponseListener<T> {
    void onComplete(Response<T> response);
}

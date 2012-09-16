package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisDataException;

public class Response<T> {
    protected T response = null;
    private boolean built = false;
    private boolean set = false;
    private Builder<T> builder;
    private Object data;
    ResponseListener<T> listener;

    public Response(Builder<T> b) {
        this.builder = b;
    }

    public synchronized void set(Object data) {
        this.data = data;
        set = true;
        if (listener != null) listener.onComplete(this);
        notify();
    }

    public synchronized void setListener(ResponseListener<T> listener) {
        this.listener = listener;
        if (set) listener.onComplete(this);
    }

    public T get() {
        if (!set) {
            throw new JedisDataException(
                    "Please close pipeline or multi block before calling this method.");
        }
        if (!built) {
        	if(data != null ){
	        	if (data instanceof JedisDataException){
	        		throw new JedisDataException((JedisDataException)data);
	        	}
	            response = builder.build(data);
        	}
            this.data = null;
            built = true;
        }
        return response;
    }

    public synchronized T await() {
        try {
            if (!set) wait();
        } catch (InterruptedException ie) {
            // were probably shutting down,
        }
        return get();
    }

    public String toString() {
        return "Response " + builder.toString();
    }

}

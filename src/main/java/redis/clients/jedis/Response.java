package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisDataException;

public class Response<T> {
    protected T response = null;
    private boolean built = false;
    private boolean set = false;
    private Builder<T> builder;
    private Object data;
    private Response<?> dependency = null;
    private boolean requestDependencyBuild = false;

    public Response(Builder<T> b) {
	this.builder = b;
    }

    public void set(Object data) {
	this.data = data;
	set = true;
    }

    public T get() {
	// if response has dependency response and dependency is not built,
	// build it first and no more!!
	if (!requestDependencyBuild && dependency != null && dependency.set
		&& !dependency.built) {
	    requestDependencyBuild = true;
	    dependency.build();
	}
	if (!set) {
	    throw new JedisDataException(
		    "Please close pipeline or multi block before calling this method.");
	}
	if (!built) {
	    build();
	}
	return response;
    }

    public void setDependency(Response<?> dependency) {
	this.dependency = dependency;
	this.requestDependencyBuild = false;
    }

    private void build() {
	if (data != null) {
	    if (data instanceof JedisDataException) {
		throw new JedisDataException((JedisDataException) data);
	    }
	    response = builder.build(data);
	}
	data = null;
	built = true;
    }

    public String toString() {
	return "Response " + builder.toString();
    }

}

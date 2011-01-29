package com.googlecode.jedis;

import com.google.common.base.Objects;

/**
 * Shard Config builder class for a {@link Jedis} instance.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
class ShardJedisConfig extends JedisConfig {

    /**
     * Get new ShardJedisConfig instance with default params.
     * 
     * @return a ShardJedisConfig instance
     */
    public static ShardJedisConfig newShardConfig() {
	return new ShardJedisConfig();
    }

    private Integer weight;

    private ShardJedisConfig() {
	super();
	weight = Sharded.DEFAULT_WEIGHT;
    }

    @Override
    public boolean equals(final Object obj) {
	if (!(obj instanceof ShardJedisConfig)) {
	    return false;
	}
	final ShardJedisConfig o = (ShardJedisConfig) obj;
	return getHost().equals(o.getHost()) && getPort().equals(o.getPort())
		&& getPassword().equals(o.getPassword())
		&& weight.equals(o.weight);
    }

    /**
     * Get weight.
     * 
     * @return weight
     */
    public int getWeight() {
	return weight;
    }

    @Override
    public ShardJedisConfig host(final String host) {
	setHost(host);
	return this;
    }

    @Override
    public ShardJedisConfig password(final String password) {
	setPassword(password);
	return this;
    }

    @Override
    public ShardJedisConfig port(final int port) {
	setPort(port);
	return this;
    }

    @Override
    public ShardJedisConfig timeout(final int timeout) {
	setTimeout(timeout);
	return this;
    }

    @Override
    public String toString() {
	return Objects.toStringHelper(ShardJedisConfig.class)
		.add("host", getHost()).add("port", getPort())
		.add("password", getPassword()).add("timeout", getTimeout())
		.add("weight", weight).toString();
    }

    /**
     * Set weight.
     * 
     * @param weight
     * @return the JedisConfig instance
     */
    public ShardJedisConfig weight(final int weight) {
	this.weight = weight;
	return this;
    }
}

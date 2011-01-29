package com.googlecode.jedis;

import java.nio.charset.Charset;

import com.google.common.base.Objects;

/**
 * Config builder class for a {@link Jedis} instance.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
public class JedisConfig {

    /**
     * Get new JedisConfig instance with default params.
     * 
     * @return a JedisConfig instance
     */
    static public JedisConfig newJedisConfig() {
	return new JedisConfig();
    }

    private Charset charset;
    private String host;
    private String password;
    private Integer port;
    private Integer timeout;

    /**
     * Use {@link #newJedisConfig()} to create a JedisConfig.
     */
    protected JedisConfig() {
	timeout = Protocol.DEFAULT_TIMEOUT;
	host = Protocol.DEFAULT_HOST;
	port = Protocol.DEFAULT_PORT;
	password = Protocol.DEFAULT_PASSWORD;
	charset = Protocol.DEFAULT_CHARSET;
    }

    /**
     * Set charset
     * 
     * @param charset
     * @return the JedisConfig instance
     */
    public JedisConfig charset(final Charset charset) {
	this.charset = charset;
	return this;
    }

    /**
     * Helper function to create a defensive copy.
     * 
     * @return a copy.
     */
    protected JedisConfig copy() {
	return newJedisConfig().charset(charset).host(host).password(password)
		.port(port).timeout(timeout);
    }

    /**
     * Get charset
     * 
     * @return the charset
     */
    public Charset getCharset() {
	return charset;
    }

    /**
     * Get host
     * 
     * @return the host
     */
    public String getHost() {
	return host;
    }

    /**
     * Get password
     * 
     * @return the password
     */
    public String getPassword() {
	return password;
    }

    /**
     * Get port
     * 
     * @return the port
     */
    public Integer getPort() {
	return port;
    }

    /**
     * Get timeout
     * 
     * @return the timeout
     */
    public Integer getTimeout() {
	return timeout;
    }

    /**
     * Set host.
     * 
     * @param host
     * @return the JedisConfig instance
     */
    public JedisConfig host(final String host) {
	this.host = host;
	return this;
    }

    /**
     * Set password
     * 
     * @param password
     * @return the JedisConfig instance
     */
    public JedisConfig password(final String password) {
	this.password = password;
	return this;
    }

    /**
     * Set port
     * 
     * @param port
     * @return the JedisConfig instance
     */
    public JedisConfig port(final int port) {
	this.port = port;
	return this;
    }

    /**
     * Set charset
     * 
     * @param charset
     *            the charset
     */
    public void setCharset(final Charset charset) {
	this.charset = charset;
    }

    /**
     * Set host
     * 
     * @param host
     *            the host
     */
    public void setHost(final String host) {
	this.host = host;
    }

    /**
     * Set password
     * 
     * @param password
     *            the password
     */
    public void setPassword(final String password) {
	this.password = password;
    }

    /**
     * Set port
     * 
     * @param port
     *            the port
     */
    public void setPort(final Integer port) {
	this.port = port;
    }

    /**
     * Set Timeout
     * 
     * @param timeout
     *            the timeout
     */
    public void setTimeout(final Integer timeout) {
	this.timeout = timeout;
    }

    /**
     * Set timeout
     * 
     * @param timeout
     *            the timeout
     * @return the JedisConfig instance
     */
    public JedisConfig timeout(final int timeout) {
	this.timeout = timeout;
	return this;
    }

    @Override
    public String toString() {
	return Objects.toStringHelper(ShardJedisConfig.class).add("host", host)
		.add("port", port).add("password", password)
		.add("timeout", timeout).toString();
    }

}
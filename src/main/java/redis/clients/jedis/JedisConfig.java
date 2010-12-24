package redis.clients.jedis;

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

    private Integer timeout;
    private String host;
    private Integer port;
    private String password;
    private Charset charset;

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
     * Helper function to create defensive copies.
     * 
     * @param jedisConfig
     * @return a copy.
     */
    protected JedisConfig copy() {
	return newJedisConfig().charset(charset).host(host).password(password)
		.port(port).timeout(timeout);
    }

    /**
     * Get charset
     * 
     * @return
     */
    public Charset getCharset() {
	return charset;
    }

    /**
     * Get host
     * 
     * @return
     */
    public String getHost() {
	return host;
    }

    /**
     * Get password
     * 
     * @return
     */
    public String getPassword() {
	return password;
    }

    /**
     * Get port
     * 
     * @return
     */
    public Integer getPort() {
	return port;
    }

    /**
     * Get timeout
     * 
     * @return
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
     */
    public void setCharset(Charset charset) {
	this.charset = charset;
    }

    /**
     * Set host
     * 
     * @param host
     */
    public void setHost(String host) {
	this.host = host;
    }

    /**
     * Set password
     * 
     * @param password
     */
    public void setPassword(String password) {
	this.password = password;
    }

    /**
     * Set port
     * 
     * @param port
     */
    public void setPort(Integer port) {
	this.port = port;
    }

    /**
     * Set Timeout
     * 
     * @param timeout
     */
    public void setTimeout(Integer timeout) {
	this.timeout = timeout;
    }

    /**
     * Set timeout
     * 
     * @param timeout
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
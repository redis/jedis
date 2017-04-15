package redis.clients.jedis;

import java.net.URI;

public class JedisServerInfo {
	private int timeout;
	private String host;
	private int port;
	private String password = null;

	@Override
	public String toString() {
		return host + ":" + port;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public JedisServerInfo(String host) {
		URI uri = URI.create(host);
		if (uri.getScheme() != null && uri.getScheme().equals("redis")) {
			this.host = uri.getHost();
			this.port = uri.getPort();
			this.password = uri.getUserInfo().split(":", 2)[1];
		} else {
			this.host = host;
			this.port = Protocol.DEFAULT_PORT;
		}
	}

	public JedisServerInfo(String host, int port, String password, int timeout) {
		this.host = host;
		this.port = port;
		this.password = password;
		this.timeout = timeout;
	}

	public JedisServerInfo(String host, int port, String password) {
		this(host, port, password, 2000);
	}

	public JedisServerInfo(String host, int port, int timeout) {
		this(host, port, null, timeout);
	}

	public JedisServerInfo(String host, int port) {
		this(host, port, null, 2000);
	}

	public JedisServerInfo(URI uri) {
		this.host = uri.getHost();
		this.port = uri.getPort();
		this.password = uri.getUserInfo().split(":", 2)[1];
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String auth) {
		this.password = auth;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}

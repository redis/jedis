package redis.clients.jedis;

public class HostAndPort {
	private String host;
	private int port;
	
	public HostAndPort(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj instanceof HostAndPort) {
			HostAndPort hp = (HostAndPort) obj;
			
			// localhost and 127.0.0.1 is same
			return port == hp.port && 
				(host.equals(hp.host) || 
						(host.equals("localhost") && hp.host.equals("127.0.0.1")) || 
						(host.equals("127.0.0.1") && hp.host.equals("localhost")) );
	    }
	    
	    return false;
	}

	@Override
	public String toString() {
	    return host + ":" + port;
	}
}

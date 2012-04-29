package redis.clients.util;

import java.net.URI;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;

public class ConnectionInfo {
	private int database;

	public ConnectionInfo(URI uri){
		if(!uri.getScheme().toLowerCase().equals("redis")){
    		throw new JedisException("Invalid URI schema " + uri.getScheme());
    	}
		
    	this.host = uri.getHost();
    	this.port = uri.getPort();
    	
    	if(uri.getUserInfo() != null){
    		String[] parts = uri.getUserInfo().split(":");
    		
    		if(!parts[0].equals("")){
        		throw new JedisException("Invalid URL: redis does not support username");
        	}
    		
    		this.password =parts[1];
    		
    		if(!uri.getPath().equals("")){
    			this.database =  Integer.parseInt(uri.getPath().substring(1));
    		}
    		else{
    			this.database = Protocol.DEFAULT_DATABASE;
    		}
    	}
	}
	
	public ConnectionInfo(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String toString() {
        return host + ":" + port;
    }

    private String host;
    private int port;
    private String password = null;
    
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
    
    public String getPassword() {
        return password;
    }

    public void setPassword(String auth) {
        this.password = auth;
    }
    
    public int getDatabase() {
        return database;
    }

}

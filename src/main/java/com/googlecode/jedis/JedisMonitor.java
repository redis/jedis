package com.googlecode.jedis;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Override to receive messages from the Monitor command.
 * 
 * @author mo
 * 
 */
public abstract class JedisMonitor {
    protected Connection connection;

    /**
     * Callback for every line written raw, optional.
     * 
     * @param line
     */
    public void onCommand(byte[] line) {
    }

    /**
     * Callback for every line written, required.
     * 
     * @param line
     */
    public abstract void onCommand(String line);

    protected void proceed(Connection connection) {
	this.connection = connection;
	do {
	    byte[] line = connection.bulkReply();
	    onCommand(line);
	    onCommand(new String(line, UTF_8));
	} while (connection.isConnected());
    }
}
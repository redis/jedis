package com.googlecode.jedis;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Override to receive messages from the Monitor command.
 * 
 * @author Jonathan Leibiusky
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
public abstract class JedisMonitor {
    protected Connection connection;

    /**
     * Callback for every line written.
     * 
     * @param line
     */
    public abstract void onCommand(String line);

    protected void proceed(final Connection connection) {
	this.connection = connection;
	do {
	    byte[] line = connection.bulkReply();
	    if (line == null) {
		line = "(nil)".getBytes(UTF_8);
	    }
	    onCommand(new String(line, UTF_8));
	} while (connection.isConnected());
    }
}
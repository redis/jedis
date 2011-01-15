package com.googlecode.jedis;

public abstract class JedisMonitor {
    protected Client client;

    public void proceed(Client client) {
        this.client = client;
        do {
            String command = client.getBulkReply();
            onCommand(command);
        } while (client.isConnected());
    }

    public abstract void onCommand(String command);
}
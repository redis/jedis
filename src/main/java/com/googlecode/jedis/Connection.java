package com.googlecode.jedis;

import java.util.List;

import com.googlecode.jedis.Protocol.Command;

interface Connection {

    byte[] bulkReply();

    void connect() throws Throwable;

    void disconnect();

    JedisConfig getJedisConfig();

    Long integerReply();

    Boolean integerReplyAsBoolean();

    boolean isConnected();

    List<byte[]> multiBulkReply();

    void rollbackTimeout();

    void sendCommand(Command cmd, byte[]... args);

    void setJedisConfig(JedisConfig jedisConfig);

    void setTimeoutInfinite();

    byte[] statusCodeReply();

    Boolean statusCodeReplyAsBoolean();

}
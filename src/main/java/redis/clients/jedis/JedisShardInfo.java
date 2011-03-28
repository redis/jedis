/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package redis.clients.jedis;

import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;

public class JedisShardInfo extends ShardInfo<Jedis> {
    public String toString() {
        return host + ":" + port + "*" + getWeight();
    }

    public String shardName(int i) {
        if (name != null)
            return name +"*"+ getWeight();
        else
            return "SHARD-" + i + "-NODE-";

    }

    private int timeout;
    private String host;
    private int port;
    private String password = null;
    private String name = null;
    private int retries = 10;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public JedisShardInfo(String host) {
        this(host, Protocol.DEFAULT_PORT);
    }
    public JedisShardInfo(String host, String name) {
        this(host, Protocol.DEFAULT_PORT, name);
    }

    public JedisShardInfo(String host, int port) {
        this(host, port, 2000);
    }

    public JedisShardInfo(String host, int port, String name) {
        this(host, port, 2000, name);
    }

    public JedisShardInfo(String host, int port, int timeout) {
        this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
    }

    public JedisShardInfo(String host, int port, int timeout, String name) {
        this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
        this.name = name;
    }

    public JedisShardInfo(String host, int port, int timeout, int weight) {
        super(weight);
        this.host = host;
        this.port = port;
        this.timeout = timeout;
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

    public String getName() {
        return this.name;
    }

    public int getRetries() {
        return this.retries;
    }

    public void setRetries(int count) {
        this.retries = count;
    }
    @Override
    public Jedis createResource() {
        return new Jedis(this);
    }
}


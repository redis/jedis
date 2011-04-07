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

    private final int timeout;
    private final String host;
    private final int port;
    private final String password;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public JedisShardInfo(String host) {
        this(host, Protocol.DEFAULT_PORT);
    }

    public JedisShardInfo(String host, int port) {
        this(host, port, null);
    }

    public JedisShardInfo(String host, int port, String password) {
        this(host, port, password, Protocol.DEFAULT_TIMEOUT); 
    }
    
    public JedisShardInfo(String host, int port, String password, int timeout) {
    	this(host, port, password, timeout, Sharded.DEFAULT_WEIGHT);
    }
    
    public JedisShardInfo(String host, int port, String password, int timeout, int weight) {
        super(weight);
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public int getTimeout() {
        return timeout;
    }

    @Override
    public Jedis createResource() {
        return new Jedis(this);
    }
}

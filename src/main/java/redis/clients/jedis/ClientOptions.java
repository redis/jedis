/*
 * Copyright 2011-2018 the original author or authors.
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

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.util.JedisURIHelper;

import java.net.URI;

/**
 * Client Options to control the behavior of {@link Jedis} or {@link JedisPool}.
 *
 * @author Mark Paluch
 */
public class ClientOptions {

    private String host;
    private int port;
    private final int connectionTimeout;
    private final int soTimeout;
    private String password;
    private int database;
    private final String clientName;
    private boolean ssl;
    private final SslOptions sslOptions;

    public ClientOptions(String host, int port, int connectionTimeout, int soTimeout, String password, int database, String clientName, boolean ssl, SslOptions sslOptions) {
        this.host = host;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
        this.password = password;
        this.database = database;
        this.clientName = clientName;
        this.ssl = ssl;
        this.sslOptions = sslOptions;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public String getPassword() {
        return password;
    }

    public int getDatabase() {
        return database;
    }

    public String getClientName() {
        return clientName;
    }

    public boolean isSsl() {
        return ssl;
    }

    public SslOptions getSslOptions() {
        return sslOptions;
    }

    public static ClientOptionsBuilder builder(){
        return new ClientOptionsBuilder();
    }

    public static ClientOptions create(){
        return builder().build();
    }

    public static class ClientOptionsBuilder {
        private static final String REDISS = "rediss";
        private String host = Protocol.DEFAULT_HOST;
        private int port = Protocol.DEFAULT_PORT;
        private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
        private int soTimeout = Protocol.DEFAULT_TIMEOUT;
        private String password;
        private int database = Protocol.DEFAULT_DATABASE;
        private String clientName;
        private boolean ssl = false;
        private SslOptions sslOptions = SslOptions.create();

        public ClientOptionsBuilder withHost(String host) {
            this.host = host;
            return this;
        }

        public ClientOptionsBuilder withPort(int port) {
            this.port = port;
            return this;
        }

        public ClientOptionsBuilder withHostAndPort(HostAndPort hostAndPort) {
            return this.withHost(hostAndPort.getHost()).withPort(hostAndPort.getPort());
        }

        public ClientOptionsBuilder withConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public ClientOptionsBuilder withSoTimeout(int soTimeout) {
            this.soTimeout = soTimeout;
            return this;
        }

        public ClientOptionsBuilder withTimeout(int timeout) {
            this.connectionTimeout = timeout;
            this.soTimeout = timeout;
            return this;
        }

        public ClientOptionsBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public ClientOptionsBuilder withDatabase(int database) {
            this.database = database;
            return this;
        }

        public ClientOptionsBuilder withClientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public ClientOptionsBuilder withSsl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public ClientOptionsBuilder withSslOptions(SslOptions sslOptions) {
            this.sslOptions = sslOptions;
            return this;
        }

        public ClientOptionsBuilder withURI(URI uri){
            if (JedisURIHelper.isValid(uri)) {
                this.host = uri.getHost();
                this.port = uri.getPort();
                this.password = JedisURIHelper.getPassword(uri);
                this.database = JedisURIHelper.getDBIndex(uri);
                this.ssl = uri.getScheme().equals(REDISS);
            } else {
                throw new InvalidURIException(String.format(
                        "Invalid Redis URI. %s", uri.toString()));
            }

            return this;
        }

        public ClientOptionsBuilder withURI(String uri) {
            return withURI(URI.create(uri));
        }

        public ClientOptions build() {
            return new ClientOptions(host, port, connectionTimeout, soTimeout, password, database, clientName, ssl, sslOptions);
        }


    }
}

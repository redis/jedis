PATH := ./redis-git/src:${PATH}

# Supported test env versions
SUPPORTED_TEST_ENV_VERSIONS := 8.2 8.0 7.4 7.2 6.2
DEFAULT_TEST_ENV_VERSION := 8.2
REDIS_ENV_WORK_DIR := $(or ${REDIS_ENV_WORK_DIR},/tmp/redis-env-work)
CLIENT_LIBS_TEST_IMAGE := redislabs/client-libs-test:8.2
TOXIPROXY_IMAGE := ghcr.io/shopify/toxiproxy:2.8.0

define REDIS1_CONF
daemonize yes
protected-mode no
port 6379
tls-port 6390
requirepass foobared
user acljedis on allcommands allkeys >fizzbuzz
user deploy on allcommands allkeys >verify
pidfile /tmp/redis1.pid
logfile /tmp/redis1.log
save ""
appendonly no
enable-module-command yes
client-output-buffer-limit pubsub 256k 128k 5
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS2_CONF
daemonize yes
protected-mode no
port 6380
requirepass foobared
pidfile /tmp/redis2.pid
logfile /tmp/redis2.log
save ""
appendonly no
endef

define REDIS3_CONF
daemonize yes
protected-mode no
port 6381
tls-port 16381
requirepass foobared
masterauth foobared
pidfile /tmp/redis3.pid
logfile /tmp/redis3.log
save ""
appendonly no
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS4_CONF
daemonize yes
protected-mode no
port 6382
tls-port 16382
requirepass foobared
masterauth foobared
pidfile /tmp/redis4.pid
logfile /tmp/redis4.log
save ""
appendonly no
slaveof localhost 6381
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS5_CONF
daemonize yes
protected-mode no
port 6383
requirepass foobared
masterauth foobared
pidfile /tmp/redis5.pid
logfile /tmp/redis5.log
save ""
appendonly no
slaveof localhost 6379
endef

define REDIS6_CONF
daemonize yes
protected-mode no
port 6384
requirepass foobared
masterauth foobared
pidfile /tmp/redis6.pid
logfile /tmp/redis6.log
save ""
appendonly no
endef

define REDIS7_CONF
daemonize yes
protected-mode no
port 6385
requirepass foobared
masterauth foobared
pidfile /tmp/redis7.pid
logfile /tmp/redis7.log
save ""
appendonly no
slaveof localhost 6384
endef

define REDIS8_CONF
daemonize yes
protected-mode no
port 6386
pidfile /tmp/redis8.pid
logfile /tmp/redis8.log
save ""
appendonly no
maxmemory-policy allkeys-lfu
endef

define REDIS9_CONF
daemonize yes
protected-mode no
port 6387
tls-port 16387
user default off
user acljedis on allcommands allkeys >fizzbuzz
pidfile /tmp/redis9.pid
logfile /tmp/redis9.log
save ""
appendonly no
client-output-buffer-limit pubsub 256k 128k 5
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS10_CONF
daemonize yes
protected-mode no
port 6388
pidfile /tmp/redis10.pid
logfile /tmp/redis10.log
save ""
appendonly no
endef

define REDIS11_CONF
daemonize yes
protected-mode no
port 6389
pidfile /tmp/redis11.pid
logfile /tmp/redis11.log
save ""
appendonly no
replicaof localhost 6388
endef

# SENTINELS
define REDIS_SENTINEL1
port 26379
tls-port 36379
daemonize yes
protected-mode no
sentinel monitor mymaster 127.0.0.1 6379 1
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 2000
sentinel failover-timeout mymaster 120000
sentinel parallel-syncs mymaster 1
pidfile /tmp/sentinel1.pid
logfile /tmp/sentinel1.log
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS_SENTINEL2
port 26380
tls-port 36380
daemonize yes
protected-mode no
sentinel monitor mymaster 127.0.0.1 6381 1
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 2000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 120000
pidfile /tmp/sentinel2.pid
logfile /tmp/sentinel2.log
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS_SENTINEL3
port 26381
daemonize yes
protected-mode no
sentinel monitor mymasterfailover 127.0.0.1 6384 1
sentinel auth-pass mymasterfailover foobared
sentinel down-after-milliseconds mymasterfailover 2000
sentinel failover-timeout mymasterfailover 120000
sentinel parallel-syncs mymasterfailover 1
pidfile /tmp/sentinel3.pid
logfile /tmp/sentinel3.log
endef

define REDIS_SENTINEL4
port 26382
tls-port 36382
daemonize yes
protected-mode no
sentinel monitor mymaster 127.0.0.1 6381 1
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 2000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 120000
pidfile /tmp/sentinel4.pid
logfile /tmp/sentinel4.log
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS_SENTINEL5
port 26383
tls-port 36383
daemonize yes
protected-mode no
user default off
user sentinel on allcommands allkeys allchannels >foobared
sentinel monitor aclmaster 127.0.0.1 6387 1
sentinel auth-user aclmaster acljedis
sentinel auth-pass aclmaster fizzbuzz
sentinel down-after-milliseconds aclmaster 2000
sentinel failover-timeout aclmaster 120000
sentinel parallel-syncs aclmaster 1
pidfile /tmp/sentinel5.pid
logfile /tmp/sentinel5.log
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

# CLUSTER REDIS NODES
define REDIS_CLUSTER_NODE1_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7379
tls-port 8379
cluster-node-timeout 150
pidfile /tmp/redis_cluster_node1.pid
logfile /tmp/redis_cluster_node1.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node1.conf
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS_CLUSTER_NODE2_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7380
tls-port 8380
cluster-node-timeout 150
pidfile /tmp/redis_cluster_node2.pid
logfile /tmp/redis_cluster_node2.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node2.conf
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS_CLUSTER_NODE3_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7381
tls-port 8381
cluster-node-timeout 150
pidfile /tmp/redis_cluster_node3.pid
logfile /tmp/redis_cluster_node3.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node3.conf
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS_CLUSTER_NODE4_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7382
tls-port 8382
cluster-node-timeout 150
pidfile /tmp/redis_cluster_node4.pid
logfile /tmp/redis_cluster_node4.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node4.conf
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

define REDIS_CLUSTER_NODE5_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7383
tls-port 8383
cluster-node-timeout 150
pidfile /tmp/redis_cluster_node5.pid
logfile /tmp/redis_cluster_node5.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node5.conf
tls-auth-clients no
tls-cert-file "src/test/resources/private.crt"
tls-key-file "src/test/resources/private.key"
tls-ca-cert-file "src/test/resources/private.crt"
endef

# STABLE CLUSTER REDIS NODES
# The structure of this cluster is not changed by the tests!
define REDIS_STABLE_CLUSTER_NODE1_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7479
cluster-node-timeout 150
pidfile /tmp/redis_stable_cluster_node1.pid
logfile /tmp/redis_stable_cluster_node1.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_stable_cluster_node1.conf
endef

define REDIS_STABLE_CLUSTER_NODE2_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7480
cluster-node-timeout 150
pidfile /tmp/redis_stable_cluster_node2.pid
logfile /tmp/redis_stable_cluster_node2.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_stable_cluster_node2.conf
endef

define REDIS_STABLE_CLUSTER_NODE3_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7481
cluster-node-timeout 150
pidfile /tmp/redis_stable_cluster_node3.pid
logfile /tmp/redis_stable_cluster_node3.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_stable_cluster_node3.conf
endef

# UDS REDIS NODES
define REDIS_UDS
daemonize yes
protected-mode no
port 0
pidfile /tmp/redis_uds.pid
logfile /tmp/redis_uds.log
unixsocket /tmp/redis_uds.sock
unixsocketperm 777
save ""
appendonly no
endef

# UNAVAILABLE REDIS NODES
define REDIS_UNAVAILABLE_CONF
daemonize yes
protected-mode no
port 6400
pidfile /tmp/redis_unavailable.pid
logfile /tmp/redis_unavailable.log
save ""
appendonly no
endef

export REDIS1_CONF
export REDIS2_CONF
export REDIS3_CONF
export REDIS4_CONF
export REDIS5_CONF
export REDIS6_CONF
export REDIS7_CONF
export REDIS8_CONF
export REDIS9_CONF
export REDIS10_CONF
export REDIS11_CONF
export REDIS_SENTINEL1
export REDIS_SENTINEL2
export REDIS_SENTINEL3
export REDIS_SENTINEL4
export REDIS_SENTINEL5
export REDIS_CLUSTER_NODE1_CONF
export REDIS_CLUSTER_NODE2_CONF
export REDIS_CLUSTER_NODE3_CONF
export REDIS_CLUSTER_NODE4_CONF
export REDIS_CLUSTER_NODE5_CONF
export REDIS_STABLE_CLUSTER_NODE1_CONF
export REDIS_STABLE_CLUSTER_NODE2_CONF
export REDIS_STABLE_CLUSTER_NODE3_CONF
export REDIS_UDS
export REDIS_UNAVAILABLE_CONF


start: cleanup compile-module
	export TEST_ENV_PROVIDER=local
	echo "$$REDIS1_CONF" | redis-server -
	echo "$$REDIS2_CONF" | redis-server -
	echo "$$REDIS3_CONF" | redis-server -
	echo "$$REDIS4_CONF" | redis-server -
	echo "$$REDIS5_CONF" | redis-server -
	echo "$$REDIS6_CONF" | redis-server -
	echo "$$REDIS7_CONF" | redis-server -
	echo "$$REDIS8_CONF" | redis-server -
	echo "$$REDIS9_CONF" | redis-server -
	echo "$$REDIS10_CONF" | redis-server -
	echo "$$REDIS11_CONF" | redis-server -
	echo "$$REDIS_SENTINEL1" > /tmp/sentinel1.conf && redis-server /tmp/sentinel1.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_SENTINEL2" > /tmp/sentinel2.conf && redis-server /tmp/sentinel2.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_SENTINEL3" > /tmp/sentinel3.conf && redis-server /tmp/sentinel3.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_SENTINEL4" > /tmp/sentinel4.conf && redis-server /tmp/sentinel4.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_SENTINEL5" > /tmp/sentinel5.conf && redis-server /tmp/sentinel5.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_CLUSTER_NODE1_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE2_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE3_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE4_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE5_CONF" | redis-server -
	echo "$$REDIS_STABLE_CLUSTER_NODE1_CONF" | redis-server -
	echo "$$REDIS_STABLE_CLUSTER_NODE2_CONF" | redis-server -
	echo "$$REDIS_STABLE_CLUSTER_NODE3_CONF" | redis-server -
	echo "$$REDIS_UDS" | redis-server -
	echo "$$REDIS_UNAVAILABLE_CONF" | redis-server -
	redis-cli -a cluster --cluster create 127.0.0.1:7479 127.0.0.1:7480 127.0.0.1:7481 --cluster-yes
	docker run -p 6479:6379 --name jedis-stack -e PORT=6379 -d ${CLIENT_LIBS_TEST_IMAGE}
	docker network create jedis-tests-net
	docker run -p 8474:8474 --network jedis-tests-net -p 29379-29380:29379-29380 --name jedis-tests-toxiproxy -d ${TOXIPROXY_IMAGE}
	docker run -p 9379:9379 --network jedis-tests-net -e PORT=9379 --name redis-failover-1 -d ${CLIENT_LIBS_TEST_IMAGE}
	docker run -p 9380:9380 --network jedis-tests-net -e PORT=9380 --name redis-failover-2 -d ${CLIENT_LIBS_TEST_IMAGE}

cleanup:
	- rm -vf /tmp/redis_cluster_node*.conf 2>/dev/null
	- rm -vf /tmp/redis_stable_cluster_node*.conf 2>/dev/null
	- rm -vf /tmp/redis_cluster_node*.log 2>/dev/null
	- rm -vf /tmp/redis_stable_cluster_node*.log 2>/dev/null
	- rm -vf /tmp/redis*.log 2>/dev/null
	- rm -vf /tmp/sentinel*.conf 2>/dev/null
	- rm dump.rdb appendonly.aof - 2>/dev/null

stop:
	@for pidfile in \
		/tmp/redis{1..11}.pid \
		/tmp/sentinel{1..5}.pid \
		/tmp/redis_cluster_node{1..5}.pid \
		/tmp/redis_stable_cluster_node{1..3}.pid \
		/tmp/redis_uds.pid; do \
		if [ -f $$pidfile ]; then \
			pid=$$(cat $$pidfile); \
			if kill -0 $$pid 2>/dev/null; then \
				echo "Stopping process $$pid from $$pidfile"; \
				kill $$pid; \
				sleep 1; \
				if kill -0 $$pid 2>/dev/null; then \
					echo "PID $$pid did not exit, forcing kill"; \
					kill -9 $$pid; \
				fi; \
			fi; \
			rm -f $$pidfile; \
		fi; \
	done
	[ -f /tmp/redis_unavailable.pid ] && kill `cat /tmp/redis_unavailable.pid` || true
	docker rm -f jedis-stack || true
	docker rm -f jedis-tests-toxiproxy || true
	docker rm -f redis-failover-1 || true
	docker rm -f redis-failover-2 || true
	docker network rm jedis-tests-net 2>/dev/null || true

test: | start mvn-test-local stop

mvn-test-local:
	@TEST_ENV_PROVIDER=local mvn -Dwith-param-names=true -Dtest=${TEST} clean verify

mvn-test:
	mvn -Dwith-param-names=true -Dtest=${TEST} clean verify

package: | start mvn-package stop

mvn-package:
	mvn clean package

deploy: | start mvn-deploy stop

mvn-deploy:
	mvn clean deploy

format:
	mvn java-formatter:format

release: | start mvn-release stop

mvn-release:
	mvn release:clean
	mvn release:prepare
	mvn release:perform -DskipTests

system-setup:
	# Install gcc with Homebrew (macOS) or apt (Linux)
	if [ "$(shell uname)" = "Darwin" ]; then \
		brew install gcc || true; \
	else \
		sudo apt install -y gcc g++; \
	fi
	[ ! -e redis-git ] && git clone https://github.com/redis/redis.git --branch unstable --single-branch redis-git || true
	$(MAKE) -C redis-git clean
	$(MAKE) -C redis-git BUILD_TLS=yes

compile-module:
	gcc -shared -o /tmp/testmodule.so -fPIC src/test/resources/testmodule.c

# Start test environment with specific version using predefined docker compose setup

start-test-env:
	@if [ -z "$(version)" ]; then \
		version=$(arg); \
		if [ -z "$$version" ]; then \
			version="$(DEFAULT_TEST_ENV_VERSION)"; \
		fi; \
	fi; \
	if ! echo "$(SUPPORTED_TEST_ENV_VERSIONS)" | grep -qw "$$version"; then \
		echo "Error: Invalid version '$$version'. Supported versions are: $(SUPPORTED_TEST_ENV_VERSIONS)."; \
		exit 1; \
	fi; \
    default_env_file="src/test/resources/env/.env"; \
	custom_env_file="src/test/resources/env/.env.v$$version"; \
	env_files="--env-file $$default_env_file"; \
	if [ -f "$$custom_env_file" ]; then \
		env_files="$$env_files --env-file $$custom_env_file"; \
	fi; \
	rm -rf "$(REDIS_ENV_WORK_DIR)"; \
	mkdir -p "$(REDIS_ENV_WORK_DIR)"; \
	docker compose $$env_files -f src/test/resources/env/docker-compose.yml up -d; \
    echo "Started test environment with Redis version $$version. "

# Stop the test environment
stop-test-env:
	docker compose -f src/test/resources/env/docker-compose.yml down; \
	rm -rf "$(REDIS_ENV_WORK_DIR)"; \
	echo "Stopped test environment and performed cleanup."

test-on-docker: | start-test-env mvn-test stop-test-env

.PHONY: test

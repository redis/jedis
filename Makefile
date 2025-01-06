PATH := ./redis-git/src:${PATH}
STUNNEL_BIN := $(shell which stunnel)

define REDIS1_CONF
daemonize yes
protected-mode no
port 6379
requirepass foobared
user acljedis on allcommands allkeys >fizzbuzz
user deploy on allcommands allkeys >verify
pidfile /tmp/redis1.pid
logfile /tmp/redis1.log
save ""
appendonly no
enable-module-command yes
client-output-buffer-limit pubsub 256k 128k 5
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
requirepass foobared
masterauth foobared
pidfile /tmp/redis3.pid
logfile /tmp/redis3.log
save ""
appendonly no
endef

define REDIS4_CONF
daemonize yes
protected-mode no
port 6382
requirepass foobared
masterauth foobared
pidfile /tmp/redis4.pid
logfile /tmp/redis4.log
save ""
appendonly no
slaveof localhost 6381
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
user default off
user acljedis on allcommands allkeys >fizzbuzz
pidfile /tmp/redis9.pid
logfile /tmp/redis9.log
save ""
appendonly no
client-output-buffer-limit pubsub 256k 128k 5
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
daemonize yes
protected-mode no
sentinel monitor mymaster 127.0.0.1 6379 1
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 2000
sentinel failover-timeout mymaster 120000
sentinel parallel-syncs mymaster 1
pidfile /tmp/sentinel1.pid
logfile /tmp/sentinel1.log
endef

define REDIS_SENTINEL2
port 26380
daemonize yes
protected-mode no
sentinel monitor mymaster 127.0.0.1 6381 1
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 2000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 120000
pidfile /tmp/sentinel2.pid
logfile /tmp/sentinel2.log
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
daemonize yes
protected-mode no
sentinel monitor mymaster 127.0.0.1 6381 1
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 2000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 120000
pidfile /tmp/sentinel4.pid
logfile /tmp/sentinel4.log
endef

define REDIS_SENTINEL5
port 26383
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
endef

# CLUSTER REDIS NODES
define REDIS_CLUSTER_NODE1_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7379
cluster-node-timeout 15000
pidfile /tmp/redis_cluster_node1.pid
logfile /tmp/redis_cluster_node1.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node1.conf
endef

define REDIS_CLUSTER_NODE2_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7380
cluster-node-timeout 15000
pidfile /tmp/redis_cluster_node2.pid
logfile /tmp/redis_cluster_node2.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node2.conf
endef

define REDIS_CLUSTER_NODE3_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7381
cluster-node-timeout 15000
pidfile /tmp/redis_cluster_node3.pid
logfile /tmp/redis_cluster_node3.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node3.conf
endef

define REDIS_CLUSTER_NODE4_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7382
cluster-node-timeout 15000
pidfile /tmp/redis_cluster_node4.pid
logfile /tmp/redis_cluster_node4.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node4.conf
endef

define REDIS_CLUSTER_NODE5_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7383
cluster-node-timeout 15000
pidfile /tmp/redis_cluster_node5.pid
logfile /tmp/redis_cluster_node5.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node5.conf
endef

# STABLE CLUSTER REDIS NODES
# The structure of this cluster is not changed by the tests!
define REDIS_STABLE_CLUSTER_NODE1_CONF
daemonize yes
protected-mode no
requirepass cluster
port 7479
cluster-node-timeout 15000
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
cluster-node-timeout 15000
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
cluster-node-timeout 15000
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

#STUNNEL
define STUNNEL_CONF
cert = src/test/resources/private.pem
pid = /tmp/stunnel.pid
[redis_1]
accept = 127.0.0.1:6390
connect = 127.0.0.1:6379
[redis_3]
accept = 127.0.0.1:16381
connect = 127.0.0.1:6381
[redis_4]
accept = 127.0.0.1:16382
connect = 127.0.0.1:6382
[redis_9]
accept = 127.0.0.1:16387
connect = 127.0.0.1:6387
[redis_cluster_1]
accept = 127.0.0.1:8379
connect = 127.0.0.1:7379
[redis_cluster_2]
accept = 127.0.0.1:8380
connect = 127.0.001:7380
[redis_cluster_3]
accept = 127.0.0.1:8381
connect = 127.0.001:7381
[redis_cluster_4]
accept = 127.0.0.1:8382
connect = 127.0.0.1:7382
[redis_cluster_5]
accept = 127.0.0.1:8383
connect = 127.0.0.1:7383
[redis_sentinel_5]
accept = 127.0.0.1:36383
connect = 127.0.0.1:26383
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
export STUNNEL_CONF
export STUNNEL_BIN


ifndef STUNNEL_BIN
    SKIP_SSL := !SSL*,
endif
export SKIP_SSL

start: stunnel cleanup compile-module
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
	docker run -p 6479:6379 --name jedis-stack -d redis/redis-stack-server:edge

cleanup:
	- rm -vf /tmp/redis_cluster_node*.conf 2>/dev/null
	- rm dump.rdb appendonly.aof - 2>/dev/null

stunnel:
	@if [ -e "$$STUNNEL_BIN" ]; then\
	    echo "$$STUNNEL_CONF" | stunnel -fd 0;\
	fi

stop:
	kill `cat /tmp/redis1.pid`
	kill `cat /tmp/redis2.pid`
	kill `cat /tmp/redis3.pid`
	kill `cat /tmp/redis4.pid`
	kill `cat /tmp/redis5.pid`
	kill `cat /tmp/redis6.pid`
	kill `cat /tmp/redis7.pid`
	kill `cat /tmp/redis8.pid`
	kill `cat /tmp/redis9.pid`
	kill `cat /tmp/redis10.pid`
	kill `cat /tmp/redis11.pid`
	kill `cat /tmp/sentinel1.pid`
	kill `cat /tmp/sentinel2.pid`
	kill `cat /tmp/sentinel3.pid`
	kill `cat /tmp/sentinel4.pid`
	kill `cat /tmp/sentinel5.pid`
	kill `cat /tmp/redis_cluster_node1.pid` || true
	kill `cat /tmp/redis_cluster_node2.pid` || true
	kill `cat /tmp/redis_cluster_node3.pid` || true
	kill `cat /tmp/redis_cluster_node4.pid` || true
	kill `cat /tmp/redis_cluster_node5.pid` || true
	kill `cat /tmp/redis_stable_cluster_node1.pid`
	kill `cat /tmp/redis_stable_cluster_node2.pid`
	kill `cat /tmp/redis_stable_cluster_node3.pid`
	kill `cat /tmp/redis_uds.pid` || true
	kill `cat /tmp/stunnel.pid` || true
	[ -f /tmp/redis_unavailable.pid ] && kill `cat /tmp/redis_unavailable.pid` || true
	rm -f /tmp/sentinel1.conf
	rm -f /tmp/sentinel2.conf
	rm -f /tmp/sentinel3.conf
	rm -f /tmp/sentinel4.conf
	rm -f /tmp/sentinel5.conf
	rm -f /tmp/redis_cluster_node1.conf
	rm -f /tmp/redis_cluster_node2.conf
	rm -f /tmp/redis_cluster_node3.conf
	rm -f /tmp/redis_cluster_node4.conf
	rm -f /tmp/redis_cluster_node5.conf
	rm -f /tmp/redis_stable_cluster_node1.conf
	rm -f /tmp/redis_stable_cluster_node2.conf
	rm -f /tmp/redis_stable_cluster_node3.conf
	docker rm -f jedis-stack

test: | start mvn-test stop

mvn-test:
	mvn -Dtest=${SKIP_SSL}${TEST} clean compile test

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

install-gcc:
	@if [ "$(shell uname)" = "Darwin" ]; then \
		brew install gcc; \
	else \
		sudo apt install -y gcc g++; \
	fi

system-setup: install-gcc
	[ ! -e redis-git ] && git clone https://github.com/redis/redis.git --branch unstable --single-branch redis-git || true
	$(MAKE) -C redis-git clean
	$(MAKE) -C redis-git

compile-module:
	gcc -shared -o /tmp/testmodule.so -fPIC src/test/resources/testmodule.c


.PHONY: test

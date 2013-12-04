define REDIS1_CONF
daemonize yes
port 6379
requirepass foobared
pidfile /tmp/redis1.pid
logfile /tmp/redis1.log
save ""
appendonly no
client-output-buffer-limit pubsub 256k 128k 5
endef

define REDIS2_CONF
daemonize yes
port 6380
requirepass foobared
pidfile /tmp/redis2.pid
logfile /tmp/redis2.log
save ""
appendonly no
endef

define REDIS3_CONF
daemonize yes
port 6381
requirepass foobared
pidfile /tmp/redis3.pid
logfile /tmp/redis3.log
save ""
appendonly no
endef

define REDIS4_CONF
daemonize yes
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
port 6383
requirepass foobared
masterauth foobared
pidfile /tmp/redis5.pid
logfile /tmp/redis5.log
save ""
appendonly no
slaveof localhost 6381
endef

define REDIS6_CONF
daemonize yes
port 6384
requirepass foobared
masterauth foobared
pidfile /tmp/redis6.pid
logfile /tmp/redis6.log
save ""
appendonly no
slaveof localhost 6379
endef

define REDIS_SENTINEL1
port 26379
daemonize yes
sentinel monitor mymaster 127.0.0.1 6379 1
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 3000
sentinel failover-timeout mymaster 900000
sentinel parallel-syncs mymaster 1
pidfile /tmp/sentinel1.pid
logfile /tmp/sentinel1.log
endef

define REDIS_SENTINEL2
port 26380
daemonize yes
sentinel monitor mymaster 127.0.0.1 6381 2
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 3000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 900000
pidfile /tmp/sentinel2.pid
logfile /tmp/sentinel2.log
endef

define REDIS_SENTINEL3
port 26381
daemonize yes
sentinel monitor mymaster 127.0.0.1 6381 2
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 3000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 900000
pidfile /tmp/sentinel3.pid
logfile /tmp/sentinel3.log
endef

export REDIS1_CONF
export REDIS2_CONF
export REDIS3_CONF
export REDIS4_CONF
export REDIS5_CONF
export REDIS6_CONF
export REDIS_SENTINEL1
export REDIS_SENTINEL2
export REDIS_SENTINEL3

start:
	echo "$$REDIS1_CONF" | redis-server -
	echo "$$REDIS2_CONF" | redis-server -
	echo "$$REDIS3_CONF" | redis-server -
	echo "$$REDIS4_CONF" | redis-server -
	echo "$$REDIS5_CONF" | redis-server -
	echo "$$REDIS6_CONF" | redis-server -
	echo "$$REDIS_SENTINEL1" > /tmp/sentinel1.conf && redis-server /tmp/sentinel1.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_SENTINEL2" > /tmp/sentinel2.conf && redis-server /tmp/sentinel2.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_SENTINEL3" > /tmp/sentinel3.conf && redis-server /tmp/sentinel3.conf --sentinel

stop:
	kill `cat /tmp/redis1.pid`
	kill `cat /tmp/redis2.pid`
	# this get's segfaulted by the tests
	kill `cat /tmp/redis3.pid` || true
	kill `cat /tmp/redis4.pid` || true
	kill `cat /tmp/redis5.pid` || true
	kill `cat /tmp/redis6.pid` || true
	kill `cat /tmp/sentinel1.pid`
	kill `cat /tmp/sentinel2.pid`
	kill `cat /tmp/sentinel3.pid`

test:
	make start
	mvn clean compile test
	make stop

deploy:
	make start
	mvn clean deploy
	make stop

release:
	make start
	mvn release:clean
	mvn release:prepare
	mvn release:perform
	make stop

.PHONY: test

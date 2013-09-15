define REDIS1_CONF
daemonize yes
port 6379
requirepass foobared
pidfile /tmp/redis1.pid
logfile /tmp/redis1.log
save ""
appendonly no
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
endef

define REDIS_SENTINEL1
port 26379
daemonize yes
sentinel monitor mymaster 127.0.0.1 6381 2
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 3000
sentinel failover-timeout mymaster 900000
sentinel can-failover mymaster yes
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
sentinel can-failover mymaster yes
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
sentinel can-failover mymaster yes
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 900000
pidfile /tmp/sentinel3.pid
logfile /tmp/sentinel3.log
endef

export REDIS1_CONF
export REDIS2_CONF
export REDIS3_CONF
export REDIS4_CONF
export REDIS_SENTINEL1
export REDIS_SENTINEL2
export REDIS_SENTINEL3

start:
	echo "$$REDIS1_CONF" | redis-server -
	echo "$$REDIS2_CONF" | redis-server -
	echo "$$REDIS3_CONF" | redis-server -
	echo "$$REDIS4_CONF" | redis-server -
	echo "$$REDIS_SENTINEL1" | redis-sentinel -
	echo "$$REDIS_SENTINEL2" | redis-sentinel -
	echo "$$REDIS_SENTINEL3" | redis-sentinel -

stop:
	kill `cat /tmp/redis1.pid`
	kill `cat /tmp/redis2.pid`
	# this get's segfaulted by the tests
	kill `cat /tmp/redis3.pid` || true
	kill `cat /tmp/redis4.pid`
	kill `cat /tmp/sentinel1.pid`
	kill `cat /tmp/sentinel2.pid`
	kill `cat /tmp/sentinel3.pid`

test:
	echo "$$REDIS1_CONF" | redis-server -
	echo "$$REDIS2_CONF" | redis-server -
	echo "$$REDIS3_CONF" | redis-server -
	echo "$$REDIS4_CONF" | redis-server -
	echo "$$REDIS_SENTINEL1" | redis-sentinel -
	echo "$$REDIS_SENTINEL2" | redis-sentinel -
	echo "$$REDIS_SENTINEL3" | redis-sentinel -

	mvn clean compile test

	kill `cat /tmp/redis1.pid`
	kill `cat /tmp/redis2.pid`
	# this get's segfaulted by the tests
	kill `cat /tmp/redis3.pid` || true
	kill `cat /tmp/redis4.pid`
	kill `cat /tmp/sentinel1.pid`
	kill `cat /tmp/sentinel2.pid`
	kill `cat /tmp/sentinel3.pid`

deploy:
	echo "$$REDIS1_CONF" | redis-server -
	echo "$$REDIS2_CONF" | redis-server -
	echo "$$REDIS3_CONF" | redis-server -
	echo "$$REDIS4_CONF" | redis-server -
	echo "$$REDIS_SENTINEL1" | redis-sentinel -
	echo "$$REDIS_SENTINEL2" | redis-sentinel -
	echo "$$REDIS_SENTINEL3" | redis-sentinel -

	mvn clean deploy

	kill `cat /tmp/redis1.pid`
	kill `cat /tmp/redis2.pid`
	# this get's segfaulted by the tests
	kill `cat /tmp/redis3.pid` || true
	kill `cat /tmp/redis4.pid`
	kill `cat /tmp/sentinel1.pid`
	kill `cat /tmp/sentinel2.pid`
	kill `cat /tmp/sentinel3.pid`

release:
	echo "$$REDIS1_CONF" | redis-server -
	echo "$$REDIS2_CONF" | redis-server -
	echo "$$REDIS3_CONF" | redis-server -
	echo "$$REDIS4_CONF" | redis-server -
	echo "$$REDIS_SENTINEL1" | redis-sentinel -
	echo "$$REDIS_SENTINEL2" | redis-sentinel -
	echo "$$REDIS_SENTINEL3" | redis-sentinel -

	mvn release:clean
	mvn release:prepare
	mvn release:perform

	kill `cat /tmp/redis1.pid`
	kill `cat /tmp/redis2.pid`
	# this get's segfaulted by the tests
	kill `cat /tmp/redis3.pid` || true
	kill `cat /tmp/redis4.pid`
	kill `cat /tmp/sentinel1.pid`
	kill `cat /tmp/sentinel2.pid`
	kill `cat /tmp/sentinel3.pid`

.PHONY: test

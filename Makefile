define REDIS1_CONF
daemonize yes
port 6379
requirepass foobared
pidfile /tmp/redis1.pid
endef

define REDIS2_CONF
daemonize yes
port 6380
requirepass foobared
pidfile /tmp/redis2.pid
endef


define REDIS_SENTINEL1
port 26379
daemonize yes
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel auth-pass mymaster foobared
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 900000
sentinel can-failover mymaster yes
sentinel parallel-syncs mymaster 1
pidfile /tmp/sentinel1.pid
endef

export REDIS1_CONF
export REDIS2_CONF
export REDIS_SENTINEL1
test:
	echo "$$REDIS1_CONF" | redis-server -
	echo "$$REDIS2_CONF" | redis-server -
	echo "$$REDIS_SENTINEL1" | redis-sentinel -

	mvn clean compile test

	kill `cat /tmp/redis1.pid`
	kill `cat /tmp/redis2.pid`
	kill `cat /tmp/sentinel1.pid`

.PHONY: test

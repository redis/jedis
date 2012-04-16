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

export REDIS1_CONF
export REDIS2_CONF
test:
	echo "$$REDIS1_CONF" | redis-server -
	echo "$$REDIS2_CONF" | redis-server -

	mvn clean compile test

	kill `cat /tmp/redis1.pid`
	kill `cat /tmp/redis2.pid`

.PHONY: test

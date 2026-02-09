PATH := ./redis-git/src:${PATH}

# Supported test env versions
SUPPORTED_TEST_ENV_VERSIONS := 8.6 8.4 8.2 8.0 7.4 7.2 6.2
DEFAULT_TEST_ENV_VERSION := 8.4
TOXIPROXY_IMAGE := ghcr.io/shopify/toxiproxy:2.8.0

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
export REDIS_UDS
export REDIS_UNAVAILABLE_CONF


start-local: cleanup compile-module
	# Simple local test env that provides only "standalone-0" endpoint and an instance listening on Unix socket
	export TEST_ENV_PROVIDER=oss-source
	echo "$$REDIS1_CONF" | redis-server -
	echo "$$REDIS_UDS" | redis-server -
	echo "$$REDIS_UNAVAILABLE_CONF" | redis-server -

cleanup:
	- rm -vf /tmp/redis*.log 2>/dev/null
	- rm dump.rdb appendonly.aof - 2>/dev/null

stop-local:
	@for pidfile in \
		/tmp/redis1.pid \
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

test-local: | start-local mvn-test-local stop-local

mvn-test-local:
	@TEST_ENV_PROVIDER=oss-source mvn -Dwith-param-names=true -Dtest=${TEST} clean verify

mvn-test:
	mvn -Dwith-param-names=true -Dtest=${TEST} clean verify

format:
	mvn java-formatter:format

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

start:
	@if [ -z "$(version)" ]; then \
		version=$(arg); \
		if [ -z "$$version" ]; then \
			version="$(DEFAULT_TEST_ENV_VERSION)"; \
		fi; \
	fi; \
	if [ -n "$$CLIENT_LIBS_TEST_IMAGE_TAG" ]; then \
		echo "Using custom image tag: $$CLIENT_LIBS_TEST_IMAGE_TAG"; \
		version=""; \
	elif ! echo "$(SUPPORTED_TEST_ENV_VERSIONS)" | grep -qw "$$version"; then \
		echo "Error: Invalid version '$$version'. Supported versions are: $(SUPPORTED_TEST_ENV_VERSIONS)."; \
		exit 1; \
	fi; \
	default_env_file="src/test/resources/env/.env"; \
	custom_env_file="src/test/resources/env/.env.v$$version"; \
	env_files="--env-file $$default_env_file"; \
	if [ -f "$$custom_env_file" ]; then \
		env_files="$$env_files --env-file $$custom_env_file"; \
	fi; \
	docker compose $$env_files -f src/test/resources/env/docker-compose.yml up -d --wait --quiet-pull; \
	echo "Started test environment with Redis version $$version. "

# Stop the test environment
stop:
	docker compose -f src/test/resources/env/docker-compose.yml down; \
	echo "Stopped test environment and performed cleanup."

test: | start mvn-test stop

.PHONY: test test-local start start-local stop stop-local cleanup mvn-test-local mvn-test format system-setup compile-module

format:
	mvn java-formatter:format

start: compile-module
	cd test-infra/no-cluster; docker-compose up -d --build --remove-orphans
	cd test-infra/oss-cluster; docker-compose up -d --remove-orphans

stop:
	cd test-infra/no-cluster; docker-compose down -v
	cd test-infra/oss-cluster; docker-compose down -v

mvn-test:
	echo Base tests
	mvn clean test
	echo Test commands - default protocol
	mvn -Dtest="redis.clients.jedis.commands.**" test
	echo Test commands - RESP3 protocol
	mvn -DjedisProtocol=3 -Dtest="redis.clients.jedis.commands.**" test
	echo Test module commands - default protocol
	mvn -DmodulesDocker="localhost:6379" -Dtest="redis.clients.jedis.modules.**" test
	echo Test module commands - RESP3 protocol
	mvn -DjedisProtocol=3 -DmodulesDocker="localhost:6379" -Dtest="redis.clients.jedis.modules.**" test

test: | start mvn-test stop

mvn-package:
	mvn clean package

package: | start mvn-package stop

mvn-deploy:
	mvn clean deploy

deploy: | start mvn-deploy stop

mvn-release:
	mvn release:clean
	mvn release:prepare
	mvn release:perform -DskipTests

release: | start mvn-release stop

system-setup:
	sudo apt install -y gcc g++

compile-module: system-setup
	gcc -shared -o test-infra/no-cluster/testmodule.so -fPIC src/test/resources/testmodule.c

.PHONY: test

# How to use Jedis Github Issue

* Github issues SHOULD BE USED to report bugs and for DETAILED feature requests. Everything else belongs in the [Jedis Google Group](https://groups.google.com/g/jedis_redis) or [Jedis Github Discussions](https://github.com/redis/jedis/discussions).

Please post general questions to Google Groups or Github discussions. These can be closed without response when posted to Github issues.

# How to contribute by Pull Request

1. Fork Jedis repo on github ([how to fork a repo](https://docs.github.com/en/get-started/quickstart/fork-a-repo))
2. Create a topic branch (`git checkout -b my_branch`)
3. Push to your remote branch (`git push origin my_branch`)
4. Create a pull request on github ([how to create a pull request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request))

Create a branch with meaningful name, and do not modify the master branch directly.

Please add unit tests to validate your changes work, then ensure your changes pass all unit tests.

# Jedis Test Environment

Jedis uses a Docker-based test environment as the primary method for running tests. A simplified local environment is also available for basic testing.

Jedis integration tests use many Redis instances, so we use a `Makefile` to prepare the environment.

## Quick Start (Docker - Recommended)

Start tests with `make test`. This will:
1. Start the Docker-based test environment
2. Run all tests
3. Stop and clean up the environment

Set up test environments with `make start`, tear down those environments with `make stop`.

# Jedis Test Environment Using Docker

This guide explains how to bootstrap and manage a test environment for Jedis using Docker Compose.

## Workflow Steps
1. **Start the test environment** by running `make start` (examples below).
2. **Run tests** through your IDE, Maven, or other testing tools as needed.
3. **Stop the test environment** by running `make stop`.
   - This will stop and tear down the Docker containers running the Redis service.

# Start the Test Environment Using Docker

You can bootstrap the test environment for supported versions of Redis using the provided `make` targets.

## Option 1: Using `make` Targets
To bring up the test environment for a specific Redis version use the following command:
```bash
make start version=8.0  # Replace with desired version
```
To stop test environment:
```bash
make stop
```
To run tests using the Docker environment:
```bash
make test
```

## Option 2: Using docker compose commands directly
Docker compose file can be found in `src/test/resources/env` folder.
- **Redis 8.4 (or other versions without custom env file)**
```bash
rm -rf /tmp/redis-env-work
export REDIS_VERSION=8.4
docker compose --env-file .env -f src/test/resources/env/docker-compose.yml up
```
- **Redis 7.4, 7.2, 6.2 (versions with custom env files)**
```bash
rm -rf /tmp/redis-env-work
export REDIS_VERSION=6.2
docker compose --env-file .env --env-file .env.v6.2 -f src/test/resources/env/docker-compose.yml up
```

# Local Test Environment (Simplified)

For basic testing with a minimal local Redis setup (requires Redis to be installed locally):

```bash
make start-local   # Start local Redis instances (standalone + Unix socket)
make test-local    # Run tests against local environment
make stop-local    # Stop local Redis instances
```

**Note:** The local environment provides only the `standalone-0` endpoint and a Unix socket instance. For full test coverage, use the Docker-based environment.


# Some rules of Jedis source code

## Code Convention

* Jedis uses HBase Formatter introduced by [HBASE-5961](https://issues.apache.org/jira/browse/HBASE-5961)
* You can import code style file (located to hbase-formatter.xml) to Eclipse, IntelliJ
  * line break by column count seems not working with IntelliJ
* <strike>You can run ```make format``` anytime to reformat without IDEs</strike>
* DO NOT format the source codes within `io.redis.examples` test package.
* A test class name MUST NOT end with `Example`.

## Adding commands

* Jedis uses many interfaces to structure commands
  * planned to write documentation about it, contribution is more welcome!
* We need to add commands to all interfaces which have responsibility to expose
  * ex) We need to add ping() command to BasicCommands, and provide implementation to all of classes which implemented BasicCommands

## type <-> byte array conversion

* string <-> byte array : use SafeEncoder.encode()
  * Caution: use String.toBytes() directly will break GBK support!
* boolean, int, long, double -> byte array : use Protocol.toByteArray()

Thanks!

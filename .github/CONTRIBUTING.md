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

Jedis unit tests run with the latest [Redis unstable branch](https://github.com/redis/redis/tree/unstable).
Please let them prepared and installed.

Jedis unit tests use many Redis instances, so we use a ```Makefile``` to prepare environment. 

Start unit tests with ```make test```.
Set up test environments with ```make start```, tear down those environments with ```make stop``` and clean up the environment files with ```make cleanup```.

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

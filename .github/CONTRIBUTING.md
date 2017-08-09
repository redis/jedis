# How to use Jedis Github Issue

* Github issues SHOULD ONLY BE USED to report bugs, and for DETAILED feature requests. Everything else belongs to the Jedis Google Group.

Jedis Google Group address:
  
https://groups.google.com/forum/?fromgroups#!forum/jedis_redis

Please post General questions to Google Group. It can be closed without answer when posted to Github issue.

# Some rules of Jedis source code

## Code Convention

* Jedis uses ```HBase Formatter``` introduced by [HBASE-5961](https://issues.apache.org/jira/browse/HBASE-5961)
* You can import code style file (located to hbase-formatter.xml) to Eclipse, IntelliJ
  * line break by column count seems not working with IntelliJ
* You can run ```make format``` anytime to reformat without IDEs

## Adding commands

* Jedis uses many interfaces to structure commands
  * planned to write documentation about it, contribution is more welcome!
* We need to add commands to all interfaces which have responsibility to expose
  * ex) We need to add ping() command to BasicCommands, and provide implementation to all of classes which implemented BasicCommands

## type <-> byte array conversion

* string <-> byte array : use SafeEncoder.encode()
  * Caution: use String.toBytes() directly will break GBK support!
* boolean, int, long, double -> byte array : use Protocol.toByteArray()

# How to contribute by Pull Request

1. Fork Jedis on github (https://help.github.com/articles/fork-a-repo/)
2. Create a topic branch (git checkout -b my_branch)
3. Push to your branch (git push origin my_branch)
4. Post a pull request on github (https://help.github.com/articles/creating-a-pull-request/)

I recommend you to create branch with meaningful name, not modifying master branch directly.

Please add unit tests in order to prove your modification works smoothly. And please make sure your modification passes all unit tests.

# Jedis Test Environment

Jedis unit tests run with latest [```Redis unstable branch```](https://github.com/antirez/redis).
Please let them prepared and installed.

Jedis unit tests use many Redis instances, so we use ```Makefile``` to prepare environment. 

You can start test with ```make test```.
You can set up test environments by ```make start```, and tear down environments by ```make stop```.

If one or some of unit tests in current master branch of Jedis fails with Redis unstable branch, please post it to Github issue, and go ahead with other unit tests at your work.

Thanks!

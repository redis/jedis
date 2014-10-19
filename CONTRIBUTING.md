# How to use Jedis Github Issue

* Github issues SHOULD ONLY BE USED to report bugs, and for DETAILED feature requests. Everything else belongs to the Jedis Google Group.

Jedis Google Group address:
  
https://groups.google.com/forum/?fromgroups#!forum/jedis_redis

Please post General questions to Google Group. It can be closed without answer when posted to Github issue.

# Some rules of Jedis source code

## Code Convention

* Jedis uses ```Java Convention```.
  * It seems to only supported by Eclipse.
* In Eclipse (or STS), you can change properties for project
  * Java Code Style -> Formatter -> Active Profile to Java Conventions [built-in]
* reflect by selection (only changes indentation)
  * Menu -> Source -> Correct Indentation
* reflect all (don't recommend)
  * Menu -> Source -> Format
* If you're using other IDEs than Eclipse, please do your work from your IDE, and correct indentation from Eclipse

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

1. Fork Jedis on github (http://help.github.com/fork-a-repo/)
2. Create a topic branch (git checkout -b my_branch)
3. Push to your branch (git push origin my_branch)
4. Post a pull request on github (http://help.github.com/send-pull-requests/)

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
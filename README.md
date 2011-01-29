# (Fork with HUGE CHANGES of) Jedis, almost not compatible, you are warned :)

I started this fork because of the need of a complete Jedis interface, got some problems before with cglib and spring [ProxyFactoryBean](http://static.springsource.org/spring/docs/3.0.x/reference/aop-api.html#aop-pfb "AOP Proxies").


done:
no arg constructor
complete Jedis interface so you can use jdk proxies
use of [Netty](http://www.jboss.org/netty "Netty") as io layer
changed a lot method signatures (e.g. get boolean instead of 1/0)
fluent config class
changed tests to testng and hamcrest
use of slf4j
where possible return immutable lists and sets
maven 3 compatible
added a lot report plugins (findbugs, cobertura...)

to be done:
sharded (will be next)
let netty test the connection before sending a command, not the pool (via HashedWheelTimer + ping maybe)
pipelined

broken and not planed to fix (yet):
transaction
pub/sub

to use it with spring put this in a spring config:

    <bean id="jedisTarget" class="com.googlecode.jedis.JedisFactory" factory-method="newJedisInstance" scope="prototype">
        <bean id="jedisConfig" class="com.googlecode.jedis.JedisConfig">
            <property name="host" value="localhost" />
            <property name="password" value="foobared" />
            <property name="timeout" value="10000"/><!-- in millis-->
        </bean>
    </bean>

	<bean id="jedisPool" class="com.googlecode.jedis.util.JedisPoolTargetSource">
		<property name="targetBeanName" value="jedisTarget" />
		<property name="maxSize" value="25" />
	</bean>

	<bean id="jedis" class="org.springframework.aop.framework.ProxyFactoryBean">
		<property name="targetSource" ref="jedisPool" />
	</bean>

Then just use the jedis bean in your beans to speak with redis.


Jedis is a blazingly small and sane [Redis](http://github.com/antirez/redis "Redis") java client.

Jedis was conceived to be EASY to use.

Jedis is fully compatible with redis 2.0.0.

## I want to persist my objects in Redis. How can I do it?
You should definitely check [JOhm](http://github.com/xetorthio/johm "JOhm")!!!
And of course, you can always serialize it and store it.

## Is there a Groovy client?

Yes. You can use Jedis if you want, but I recommend [Gedis](http://github.com/xetorthio/gedis "Gedis"), which is Jedis but with a nicer groovy-like interface :)

## Community

Meet us on IRC: ##jedis on freenode.net

Join the mailing-list at [http://groups.google.com/group/jedis_redis](http://groups.google.com/group/jedis_redis)

## Ok.. so what can I do with Jedis?
All of the following redis features are supported:

- Sorting
- Connection handling
- Commands operating on all the kind of values
- Commands operating on string values
- Commands operating on hashes
- Commands operating on lists
- Commands operating on sets
- Commands operating on sorted sets
- Persistence control commands
- Remote server control commands
- Connection pooling

No More:
- Pipelining
- Publish/Subscribe
- Sharding (MD5, MurmureHash)
- Key-tags for sharding
- Sharding with pipelining
- Transactions

## How do I use it?

clone this and mvn install -DskipTests=true

then use it as a maven dependency:

    <dependency>
        <groupId>com.googlecode.jedis</groupId>
        <artifactId>jedis</artifactId>
        <version>2.0.1-SNAPSHOT</version>
        <type>jar</type>
    </dependency>


To use it just:

    Jedis jedis = new Jedis("localhost");
    jedis.set("foo", "bar");
    String value = jedis.get("foo");

For more usage examples check the tests.

Please check the [wiki](http://github.com/xetorthio/jedis/wiki "wiki"). There are lots of cool stuff you should know!

And you are done!

## I want to contribute!

That is great! Just fork the project in github. Create a topic branch, write some tests and the feature that you wish to contribute.

To run the tests:

- Use the latest redis master branch.

- Run 2 instances of redis using conf files in conf folder. For the tests we use 2 redis servers, one on default port (6379) and the other one on (6380). Both have authentication enabled with default password (foobared). This way we can test both sharding and auth command.

Thanks for helping!

## License

Copyright (c) 2011 Jonathan Leibiusky, Moritz Heuser

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.


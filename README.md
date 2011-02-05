# (Fork with HUGE CHANGES of) Jedis, almost not compatible, you are warned :)

I started this fork because of the need of a complete Jedis interface, got some problems before with cglib and spring [ProxyFactoryBean](http://static.springsource.org/spring/docs/3.0.x/reference/aop-api.html#aop-pfb "AOP Proxies").


Done so far:

 - no arg constructor
 - complete Jedis interface so you can use jdk proxies
 - use of [Netty](http://www.jboss.org/netty "Netty") as io layer
 - changed a lot method signatures (e.g. get boolean instead of 1/0)
 - fluent config class
 - changed tests to testng and hamcrest
 - use of slf4j
 - where possible return immutable lists and sets
 - maven 3 compatible
 - added a lot report plugins (findbugs, cobertura...)

To be done:

 - sharded (will be next)
 - let netty test the connection before sending a command, not the pool (via HashedWheelTimer + ping maybe)
 - pipelined

broken and not planed to fix (yet):
 - transaction
 - pub/sub

To use it with spring put this in a spring config:

    <bean id="jedisTarget" class="com.googlecode.jedis.JedisFactory" factory-method="newJedisInstance" scope="prototype">
    <constructor-arg>
        <bean id="jedisConfig" class="com.googlecode.jedis.JedisConfig">
            <property name="host" value="localhost" />
            <property name="password" value="foobared" />
            <property name="timeout" value="10000"/><!-- in millis-->
        </bean>
    </constructor-arg>
    </bean>

	<bean id="jedisPool" class="com.googlecode.jedis.util.JedisPoolTargetSource">
		<property name="targetBeanName" value="jedisTarget" />
		<property name="maxSize" value="25" />
	</bean>

	<bean id="jedis" class="org.springframework.aop.framework.ProxyFactoryBean">
		<property name="targetSource" ref="jedisPool" />
	</bean>

Or the much cooler Spring Java Config Way:

    @Bean()
    @Scope(value="prototype")
    public Jedis jedisTarget(){
        JedisConfig config = JedisConfig.newJedisConfig().host("localhost").password("foobared");
        return JedisFactory.newJedisInstance(config);
    }
    
    @Bean
    public JedisPoolTargetSource jedisPoolTargetSource(){
        JedisPoolTargetSource jedisPoolTargetSource = new JedisPoolTargetSource();
        jedisPoolTargetSource.setTargetClass(Jedis.class);
        jedisPoolTargetSource.setTargetBeanName("jedisTarget");
        jedisPoolTargetSource.setMaxSize(25);
        return jedisPoolTargetSource;
    }
    
    @Bean
    public ProxyFactoryBean jedisProxyFactoryBean(){
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTargetSource(jedisPoolTargetSource());
        return proxyFactoryBean;        
    }
    
    @Bean
    public Jedis jedis(){
        return (Jedis) jedisProxyFactoryBean().getObject();
    }


Then just use the jedis bean in your beans to speak with redis, e.g.:

    @Autowired
    Jedis jedis;

    void somemethod(){
        assert "foobar".equals(jedis.echo("foobar"));
    }


## How do I use it?

clone this and mvn install -DskipTests=true

then use it as a maven dependency:

    <dependency>
        <groupId>com.googlecode.jedis</groupId>
        <artifactId>jedis</artifactId>
        <version>2.0.1-SNAPSHOT</version>
        <type>jar</type>
    </dependency>

## Thanks
Big thanks to Jonathan Leibiusky and all commiters of jedis and to
the commiters of redis scala netty client.


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


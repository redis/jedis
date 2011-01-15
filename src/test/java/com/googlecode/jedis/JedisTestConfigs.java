package com.googlecode.jedis;

import com.googlecode.jedis.JedisConfig;
import com.googlecode.jedis.Protocol;

public abstract class JedisTestConfigs {

    static public JedisConfig c1 = JedisConfig.newJedisConfig().password(
	    "foobared");
    static public JedisConfig c2 = JedisConfig.newJedisConfig()
	    .password("foobared").port(Protocol.DEFAULT_PORT + 1);

}

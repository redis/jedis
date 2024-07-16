#!js api_version=1.0 name=withFlags
redis.registerFunction("my_set",
    (c, key, val) => {
        return c.call("set", key, val);
    },
    {
        flags: [redis.functionFlags.RAW_ARGUMENTS]
    }
);
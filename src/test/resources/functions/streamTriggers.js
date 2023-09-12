#!js api_version=1.0 name=streamTriggers

redis.registerStreamTrigger(
    "consumer", // consumer name
    "stream", // streams prefix
    function(c, data) {
        // callback to run on each element added to the stream
        redis.log(JSON.stringify(data, (key, value) =>
            typeof value === 'bigint'
                ? value.toString()
                : value // return everything else unchanged
        ));
    }
);
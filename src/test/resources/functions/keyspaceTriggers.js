#!js api_version=1.0 name=keyspaceTriggers

redis.registerKeySpaceTrigger("consumer", "", function(client, data){
    if (client.call("type", data.key) != "hash") {
        // key is not a hash, do not touch it.
        return;
    }
    // get the current time in ms
    var curr_time = client.call("time")[0];
    // set '__last_updated__' with the current time value
    client.call('hset', data.key, '__last_updated__', curr_time);
});
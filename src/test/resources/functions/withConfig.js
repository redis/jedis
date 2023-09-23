#!js api_version=1.0 name=withConfig

var last_modified_field_name = "__last_modified__"

if (redis.config.last_modified_field_name !== undefined) {
    if (typeof redis.config.last_modified_field_name != 'string') {
        throw "last_modified_field_name must be a string";
    }
    last_modified_field_name = redis.config.last_modified_field_name
}

redis.registerFunction("hset", function(client, key, field, val){
    // get the current time in ms
    var curr_time = client.call("time")[0];
    return client.call('hset', key, field, val, last_modified_field_name, curr_time);
});
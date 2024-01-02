#!js api_version=1.0 name=hashitout

redis.registerFunction('hashy', function(client, key_name){
    if (client.call('type', key_name) == 'hash') {
        return client.call('hgetall', key_name);
    }
    throw "Oops, that wasn't a Hash!";
});
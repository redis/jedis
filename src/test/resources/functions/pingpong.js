#!js api_version=1.0 name=pingpong

function answer(client, data) {
    return client.call('ping');
}

redis.registerFunction('playPingPong', answer, {description: 'You PING, we PONG'});
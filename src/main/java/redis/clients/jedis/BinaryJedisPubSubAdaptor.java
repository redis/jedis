package redis.clients.jedis;

public class BinaryJedisPubSubAdaptor extends BinaryJedisPubSub {
	@Override
	public void onMessage(byte[] channel, byte[] message) {

	}

	@Override
	public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {

	}

	@Override
	public void onSubscribe(byte[] channel, int subscribedChannels) {

	}

	@Override
	public void onUnsubscribe(byte[] channel, int subscribedChannels) {

	}

	@Override
	public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {

	}

	@Override
	public void onPSubscribe(byte[] pattern, int subscribedChannels) {

	}
}

package com.googlecode.jedis;


public class PublishSubscribeCommandsTest extends JedisTestBase {
    // @Test
    // public void psubscribe() throws UnknownHostException, IOException,
    // InterruptedException {
    // Thread t = new Thread(new Runnable() {
    // public void run() {
    // try {
    // Jedis j = JedisFactory.newJedisInstance(c1);
    // Thread.sleep(1000);
    // j.publish("foo.bar", "exit");
    // j.disconnect();
    // } catch (Exception ex) {
    // fail(ex.getMessage());
    // }
    // }
    // });
    // t.start();
    // jedis.psubscribe(new JedisPubSub() {
    // @Override
    // public void onMessage(String channel, String message) {
    // }
    //
    // @Override
    // public void onPMessage(String pattern, String channel,
    // String message) {
    // assertThat(pattern, is("foo.*"));
    // assertThat(channel, is("foo.bar"));
    // assertThat(message, is("exit"));
    // punsubscribe();
    // }
    //
    // @Override
    // public void onPSubscribe(String pattern, int subscribedChannels) {
    // assertThat(pattern, is("foo.*"));
    // assertThat(subscribedChannels, is(1));
    // }
    //
    // @Override
    // public void onPUnsubscribe(String pattern, int subscribedChannels) {
    // assertThat(pattern, is("foo.*"));
    // assertThat(subscribedChannels, is(0));
    // }
    //
    // @Override
    // public void onSubscribe(String channel, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onUnsubscribe(String channel, int subscribedChannels) {
    // }
    // }, "foo.*");
    // t.join();
    // }
    //
    // @Test
    // public void psubscribeMany() throws UnknownHostException, IOException,
    // InterruptedException {
    // Thread t = new Thread(new Runnable() {
    // public void run() {
    // try {
    // Jedis j = JedisFactory.newJedisInstance(c1);
    // Thread.sleep(1000);
    // j.publish("foo.123", "exit");
    // Thread.sleep(1000);
    // j.publish("bar.123", "exit");
    // j.disconnect();
    // } catch (Exception ex) {
    // fail(ex.getMessage());
    // }
    // }
    // });
    // t.start();
    // jedis.psubscribe(new JedisPubSub() {
    // @Override
    // public void onMessage(String channel, String message) {
    // }
    //
    // @Override
    // public void onPMessage(String pattern, String channel,
    // String message) {
    // punsubscribe(pattern);
    // }
    //
    // @Override
    // public void onPSubscribe(String pattern, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onPUnsubscribe(String pattern, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onSubscribe(String channel, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onUnsubscribe(String channel, int subscribedChannels) {
    // }
    // }, "foo.*", "bar.*");
    // t.join();
    // }
    //
    // @Test
    // public void subscribe() throws UnknownHostException, IOException,
    // InterruptedException {
    // Thread t = new Thread(new Runnable() {
    // public void run() {
    // try {
    // Jedis j = JedisFactory.newJedisInstance(c1);
    // Thread.sleep(1000);
    // j.publish("foo", "exit");
    // j.disconnect();
    // } catch (Exception ex) {
    // fail(ex.getMessage());
    // }
    // }
    // });
    // t.start();
    // jedis.subscribe(new JedisPubSub() {
    // @Override
    // public void onMessage(String channel, String message) {
    // assertThat(channel, is("foo"));
    // assertThat(message, is("exit"));
    // unsubscribe();
    // }
    //
    // @Override
    // public void onPMessage(String pattern, String channel,
    // String message) {
    // }
    //
    // @Override
    // public void onPSubscribe(String pattern, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onPUnsubscribe(String pattern, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onSubscribe(String channel, int subscribedChannels) {
    // assertThat(channel, is("foo"));
    // assertThat(subscribedChannels, is(1));
    // }
    //
    // @Override
    // public void onUnsubscribe(String channel, int subscribedChannels) {
    // assertThat(channel, is("foo"));
    // assertThat(subscribedChannels, is(0));
    // }
    // }, "foo");
    // t.join();
    // }
    //
    // @Test
    // public void subscribeLazily() throws UnknownHostException, IOException,
    // InterruptedException {
    // final JedisPubSub pubsub = new JedisPubSub() {
    // @Override
    // public void onMessage(String channel, String message) {
    // unsubscribe(channel);
    // }
    //
    // @Override
    // public void onPMessage(String pattern, String channel,
    // String message) {
    // punsubscribe(pattern);
    // }
    //
    // @Override
    // public void onPSubscribe(String pattern, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onPUnsubscribe(String pattern, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onSubscribe(String channel, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onUnsubscribe(String channel, int subscribedChannels) {
    // }
    // };
    //
    // Thread t = new Thread(new Runnable() {
    // public void run() {
    // try {
    // Jedis j = JedisFactory.newJedisInstance(c1);
    // Thread.sleep(1000);
    // pubsub.subscribe("bar");
    // pubsub.psubscribe("bar.*");
    // j.publish("foo", "exit");
    // j.publish("bar", "exit");
    // j.publish("bar.123", "exit");
    // j.disconnect();
    // } catch (Exception ex) {
    // fail(ex.getMessage());
    // }
    // }
    // });
    // t.start();
    // jedis.subscribe(pubsub, "foo");
    // t.join();
    // }
    //
    // @Test
    // public void subscribeMany() throws UnknownHostException, IOException,
    // InterruptedException {
    // Thread t = new Thread(new Runnable() {
    // public void run() {
    // try {
    // Jedis j = JedisFactory.newJedisInstance(c1);
    // Thread.sleep(1000);
    // j.publish("foo", "exit");
    // Thread.sleep(1000);
    // j.publish("bar", "exit");
    // j.disconnect();
    // } catch (Exception ex) {
    // fail(ex.getMessage());
    // }
    // }
    // });
    // t.start();
    // jedis.subscribe(new JedisPubSub() {
    // @Override
    // public void onMessage(String channel, String message) {
    // unsubscribe(channel);
    // }
    //
    // @Override
    // public void onPMessage(String pattern, String channel,
    // String message) {
    // }
    //
    // @Override
    // public void onPSubscribe(String pattern, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onPUnsubscribe(String pattern, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onSubscribe(String channel, int subscribedChannels) {
    // }
    //
    // @Override
    // public void onUnsubscribe(String channel, int subscribedChannels) {
    // }
    // }, "foo", "bar");
    // t.join();
    // }
}
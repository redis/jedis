package com.googlecode.jedis;


public class PipelineTest extends JedisTestBase {

    String foo = "foo";
    String bar = "bar";

    // @Test
    // public void pipelined() {
    // jedis.pipelined();
    // assertThat(jedis.set(foo, bar), is(false));
    // assertThat(jedis.get(foo), nullValue());
    // List<byte[]> l = jedis.executeRaw();
    //
    // assertThat(l, notNullValue());
    // assertThat(l.size(), is(2));
    // assertThat(l.get(0), is("OK".getBytes(UTF_8)));
    // assertThat(l.get(1), is(bar.getBytes(UTF_8)));
    //
    // assertThat(jedis.get(foo), is(bar));
    // }
}

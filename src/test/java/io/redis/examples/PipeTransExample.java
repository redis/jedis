// EXAMPLE: pipe_trans_tutorial
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;
// REMOVE_END
import java.util.List;

import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.AbstractPipeline;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.Response;

public class PipeTransExample {

    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");

        // REMOVE_START
        for (int i = 0; i < 5; i++) {
            jedis.del(String.format("seat:%d", i));
        }

        jedis.del("counter:1", "counter:2", "counter:3", "shellpath");
        // REMOVE_END

        // STEP_START basic_pipe
        AbstractPipeline pipe = jedis.pipelined();

        for (int i = 0; i < 5; i++) {
            pipe.set(String.format("seat:%d", i), String.format("#%d", i));
        }

        pipe.sync();

        pipe = jedis.pipelined();

        Response<String> resp0 = pipe.get("seat:0");
        Response<String> resp3 = pipe.get("seat:3");
        Response<String> resp4 = pipe.get("seat:4");

        pipe.sync();

        // Responses are available after the pipeline has executed.
        System.out.println(resp0.get()); // >>> #0
        System.out.println(resp3.get()); // >>> #3
        System.out.println(resp4.get()); // >>> #4
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("#0", resp0.get());
        Assert.assertEquals("#3", resp3.get());
        Assert.assertEquals("#4", resp4.get());
        // REMOVE_END

        // STEP_START basic_trans
        AbstractTransaction trans = jedis.multi();

        trans.incrBy("counter:1", 1);
        trans.incrBy("counter:2", 2);
        trans.incrBy("counter:3", 3);

        trans.exec();

        System.out.println(jedis.get("counter:1")); // >>> 1
        System.out.println(jedis.get("counter:2")); // >>> 2
        System.out.println(jedis.get("counter:3")); // >>> 3
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("1", jedis.get("counter:1"));
        Assert.assertEquals("2", jedis.get("counter:2"));
        Assert.assertEquals("3", jedis.get("counter:3"));
        // REMOVE_END

        // STEP_START trans_watch
        // Set initial value of `shellpath`.
        jedis.set("shellpath", "/usr/syscmds/");

        // Start the transaction and watch the key we are about to update.
        trans = jedis.transaction(false); // create a Transaction object without sending MULTI command
        trans.watch("shellpath"); // send WATCH command(s)
        trans.multi(); // send MULTI command

        String currentPath = jedis.get("shellpath");
        String newPath = currentPath + ":/usr/mycmds/";

        // Commands added to the `trans` object
        // will be buffered until `trans.exec()` is called.
        Response<String> setResult = trans.set("shellpath", newPath);
        List<Object> transResults = trans.exec();
        
        // The `exec()` call returns null if the transaction failed.
        if (transResults != null) {
            // Responses are available if the transaction succeeded.
            System.out.println(setResult.get()); // >>> OK

            // You can also get the results from the list returned by
            // `trans.exec()`.
            for (Object item: transResults) {
                System.out.println(item);
            }
            // >>> OK

            System.out.println(jedis.get("shellpath"));
            // >>> /usr/syscmds/:/usr/mycmds/
        }
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("/usr/syscmds/:/usr/mycmds/", jedis.get("shellpath"));
        Assert.assertEquals("OK", setResult.get());
        Assert.assertEquals(1, transResults.size());
        Assert.assertEquals("OK", transResults.get(0).toString());
        // REMOVE_END

// HIDE_START
        jedis.close();
    }   
}
// HIDE_END
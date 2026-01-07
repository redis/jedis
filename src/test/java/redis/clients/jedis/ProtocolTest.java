package redis.clients.jedis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.search.CombineArgs;
import redis.clients.jedis.search.HybridArgs;
import redis.clients.jedis.search.HybridSearchArgs;
import redis.clients.jedis.search.HybridVectorArgs;
import redis.clients.jedis.search.PostProcessingArgs;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.util.FragmentedByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayListEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.exceptions.JedisBusyException;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

public class ProtocolTest {
  @Test
  public void buildACommand() throws IOException {
    PipedInputStream pis = new PipedInputStream();
    BufferedInputStream bis = new BufferedInputStream(pis);
    PipedOutputStream pos = new PipedOutputStream(pis);
    RedisOutputStream ros = new RedisOutputStream(pos);

//    Protocol.sendCommand(ros, Protocol.Command.GET, "SOMEKEY".getBytes(Protocol.CHARSET));
    Protocol.sendCommand(ros, new CommandArguments(Protocol.Command.GET).add("SOMEKEY"));
    ros.flush();
    pos.close();
    String expectedCommand = "*2\r\n$3\r\nGET\r\n$7\r\nSOMEKEY\r\n";

    int b;
    StringBuilder sb = new StringBuilder();
    while ((b = bis.read()) != -1) {
      sb.append((char) b);
    }

    assertEquals(expectedCommand, sb.toString());
  }

  @Test
  public void writeOverflow() throws IOException {
    RedisOutputStream ros = new RedisOutputStream(new OutputStream() {

      @Override
      public void write(int b) throws IOException {
        throw new IOException("thrown exception");

      }
    });

    ros.write(new byte[8191]);

    try {
      ros.write((byte) '*');
    } catch (IOException ioe) {
      //ignore
    }
    assertThrows(IOException.class, ()-> ros.write((byte) '*'));
  }

  @Test
  public void bulkReply() {
    InputStream is = new ByteArrayInputStream("$6\r\nfoobar\r\n".getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(is));
    assertArrayEquals(SafeEncoder.encode("foobar"), response);
  }

  @Test
  public void fragmentedBulkReply() {
    FragmentedByteArrayInputStream fis = new FragmentedByteArrayInputStream(
        "$30\r\n012345678901234567890123456789\r\n".getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(fis));
    assertArrayEquals(SafeEncoder.encode("012345678901234567890123456789"), response);
  }

  @Test
  public void nullBulkReply() {
    InputStream is = new ByteArrayInputStream("$-1\r\n".getBytes());
    String response = (String) Protocol.read(new RedisInputStream(is));
    assertNull(response);
  }

  @Test
  public void singleLineReply() {
    InputStream is = new ByteArrayInputStream("+OK\r\n".getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(is));
    assertArrayEquals(SafeEncoder.encode("OK"), response);
  }

  @Test
  public void integerReply() {
    InputStream is = new ByteArrayInputStream(":123\r\n".getBytes());
    long response = (Long) Protocol.read(new RedisInputStream(is));
    assertEquals(123, response);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void multiBulkReply() {
    InputStream is = new ByteArrayInputStream(
        "*4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$5\r\nWorld\r\n".getBytes());
    List<byte[]> response = (List<byte[]>) Protocol.read(new RedisInputStream(is));
    List<byte[]> expected = new ArrayList<byte[]>();
    expected.add(SafeEncoder.encode("foo"));
    expected.add(SafeEncoder.encode("bar"));
    expected.add(SafeEncoder.encode("Hello"));
    expected.add(SafeEncoder.encode("World"));
    assertByteArrayListEquals(expected, response);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void nullMultiBulkReply() {
    InputStream is = new ByteArrayInputStream("*-1\r\n".getBytes());
    List<String> response = (List<String>) Protocol.read(new RedisInputStream(is));
    assertNull(response);
  }

  @Test
  public void busyReply() {
    final String busyMessage = "BUSY Redis is busy running a script.";
    final InputStream is = new ByteArrayInputStream(('-' + busyMessage + "\r\n").getBytes());
    try {
      Protocol.read(new RedisInputStream(is));
    } catch (final JedisBusyException e) {
      assertEquals(busyMessage, e.getMessage());
      return;
    }
    fail("Expected a JedisBusyException to be thrown.");
  }

  @Test
  public void buildFtHybridCommand() throws IOException {
    PipedInputStream pis = new PipedInputStream();
    BufferedInputStream bis = new BufferedInputStream(pis);
    PipedOutputStream pos = new PipedOutputStream(pis);
    RedisOutputStream ros = new RedisOutputStream(pos);

    // Build the same comprehensive HybridArgs as in the integration test
    String indexName = "myIndex";
    byte[] queryVector = floatArrayToByteArray(new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f});

    // Test @ prefix auto-addition: use fields without @ prefix
    PostProcessingArgs postProcessing = PostProcessingArgs.builder()
        .load("price", "brand", "@category") // Mix with and without @
        .addOperation(PostProcessingArgs.GroupBy.of("brand") // No @ prefix
            .reduce(PostProcessingArgs.Reducer.of(PostProcessingArgs.ReduceFunction.SUM, "@price").as("sum"))
            .reduce(PostProcessingArgs.Reducer.of(PostProcessingArgs.ReduceFunction.COUNT).as("count")))
        .addOperation(PostProcessingArgs.SortBy.of(
            new PostProcessingArgs.SortProperty("sum", PostProcessingArgs.SortDirection.ASC), // No @ prefix
            new PostProcessingArgs.SortProperty("count", PostProcessingArgs.SortDirection.DESC))) // No @ prefix
        .addOperation(PostProcessingArgs.Apply.of("@sum * 0.9", "discounted_price"))
        .addOperation(PostProcessingArgs.Filter.of("@sum > 700"))
        .addOperation(PostProcessingArgs.Limit.of(0, 20))
        .build();

    HybridArgs hybridArgs = HybridArgs.builder()
        .search(HybridSearchArgs.builder()
            .query("@category:{electronics} smartphone camera")
            .scorer(HybridSearchArgs.Scorer.of(HybridSearchArgs.ScoringFunction.BM25))
            .scoreAlias("text_score")
            .build())
        .vectorSearch(HybridVectorArgs.builder()
            .field("@image_embedding")
            .vector(queryVector)
            .method(HybridVectorArgs.Knn.of(20).efRuntime(150))
            // Single combined filter
            .filter("@brand:{apple|samsung|google}")
            .scoreAlias("vector_score")
            .build())
        .combine(CombineArgs.of(new CombineArgs.Linear().alpha(0.7).beta(0.3)))
        .postProcessing(postProcessing)
        .param("discount_rate", "0.9")
        .build();

    CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID)
        .add(indexName)
        .addParams(hybridArgs);

    Protocol.sendCommand(ros, args);
    ros.flush();
    pos.close();

    // Expected RESP output with:
    // - Single FILTER clause
    // - @ prefix auto-added to fields in PostProcessingArgs
    String expectedCommand = "*66\r\n" +
        "$9\r\nFT.HYBRID\r\n" +
        "$7\r\nmyIndex\r\n" +
        "$6\r\nSEARCH\r\n" +
        "$41\r\n@category:{electronics} smartphone camera\r\n" +
        "$6\r\nSCORER\r\n" +
        "$4\r\nBM25\r\n" +
        "$14\r\nYIELD_SCORE_AS\r\n" +
        "$10\r\ntext_score\r\n" +
        "$4\r\nVSIM\r\n" +
        "$16\r\n@image_embedding\r\n" +
        "$40\r\n" + new String(queryVector, StandardCharsets.ISO_8859_1) + "\r\n" +
        "$3\r\nKNN\r\n" +
        "$1\r\n4\r\n" +
        "$1\r\nK\r\n" +
        "$2\r\n20\r\n" +
        "$10\r\nEF_RUNTIME\r\n" +
        "$3\r\n150\r\n" +
        // Single FILTER clause
        "$6\r\nFILTER\r\n" +
        "$29\r\n@brand:{apple|samsung|google}\r\n" +
        "$14\r\nYIELD_SCORE_AS\r\n" +
        "$12\r\nvector_score\r\n" +
        "$7\r\nCOMBINE\r\n" +
        "$6\r\nLINEAR\r\n" +
        "$1\r\n4\r\n" +
        "$5\r\nALPHA\r\n" +
        "$3\r\n0.7\r\n" +
        "$4\r\nBETA\r\n" +
        "$3\r\n0.3\r\n" +
        "$4\r\nLOAD\r\n" +
        "$1\r\n3\r\n" +
        // @ prefix auto-added
        "$6\r\n@price\r\n" +
        "$6\r\n@brand\r\n" +
        "$9\r\n@category\r\n" +
        "$7\r\nGROUPBY\r\n" +
        "$1\r\n1\r\n" +
        "$6\r\n@brand\r\n" +
        "$6\r\nREDUCE\r\n" +
        "$3\r\nSUM\r\n" +
        "$1\r\n1\r\n" +
        "$6\r\n@price\r\n" +
        "$2\r\nAS\r\n" +
        "$3\r\nsum\r\n" +
        "$6\r\nREDUCE\r\n" +
        "$5\r\nCOUNT\r\n" +
        "$1\r\n0\r\n" +
        "$2\r\nAS\r\n" +
        "$5\r\ncount\r\n" +
        "$6\r\nSORTBY\r\n" +
        "$1\r\n4\r\n" +
        // @ prefix auto-added
        "$4\r\n@sum\r\n" +
        "$3\r\nASC\r\n" +
        "$6\r\n@count\r\n" +
        "$4\r\nDESC\r\n" +
        "$5\r\nAPPLY\r\n" +
        "$10\r\n@sum * 0.9\r\n" +
        "$2\r\nAS\r\n" +
        "$16\r\ndiscounted_price\r\n" +
        "$6\r\nFILTER\r\n" +
        "$10\r\n@sum > 700\r\n" +
        "$5\r\nLIMIT\r\n" +
        "$1\r\n0\r\n" +
        "$2\r\n20\r\n" +
        "$6\r\nPARAMS\r\n" +
        "$1\r\n2\r\n" +
        "$13\r\ndiscount_rate\r\n" +
        "$3\r\n0.9\r\n";

    // Read the RESP encoding
    int b;
    StringBuilder sb = new StringBuilder();
    while ((b = bis.read()) != -1) {
      sb.append((char) b);
    }

    // Assert exact RESP encoding
    assertEquals(expectedCommand, sb.toString());
  }

  @Test
  public void testHybridArgsValidation() {
    // Test that both SEARCH and VSIM are required
    assertThrows(IllegalArgumentException.class, () -> {
      HybridArgs.builder()
          .search(HybridSearchArgs.builder().query("test").build())
          .build(); // Missing VSIM
    });

    assertThrows(IllegalArgumentException.class, () -> {
      HybridArgs.builder()
          .vectorSearch(HybridVectorArgs.builder()
              .field("@vec")
              .vector(new byte[]{1, 2, 3})
              .method(HybridVectorArgs.Knn.of(10))
              .build())
          .build(); // Missing SEARCH
    });
  }

  private static byte[] floatArrayToByteArray(float[] floats) {
    ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    for (float f : floats) {
      buffer.putFloat(f);
    }
    return buffer.array();
  }
}

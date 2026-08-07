package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.RespResponse;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * HIMPORT lifecycle at the wire level, against a mock server recording every command: PREPARE is
 * injected once per connection ahead of the first SET, and queued DISCARDs of closed templates go
 * out right before the connection's next command. Command formats accepted by a real server are
 * covered by the command ITs and {@code RedisClientHashImportLifecycleIT}.
 */
public class HimportLifecycleMockTest {

  private TcpMockServer mockServer;
  private Connection connection;
  private CommandObjects commandObjects;
  private final List<String> received = Collections.synchronizedList(new ArrayList<>());

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.setCommandHandler((args, clientId) -> {
      StringBuilder command = new StringBuilder();
      for (Rawable arg : args) {
        if (command.length() > 0) {
          command.append(' ');
        }
        command.append(SafeEncoder.encode(arg.getRaw()));
      }
      received.add(command.toString());
      return RespResponse.simpleString("OK");
    });
    mockServer.start();
    connection = new Connection("localhost", mockServer.getPort());
    commandObjects = new CommandObjects(RedisProtocol.RESP2);
  }

  @AfterEach
  public void tearDown() throws IOException {
    connection.close();
    mockServer.stop();
  }

  @Test
  public void preparesOncePerConnectionAheadOfFirstSet() {
    try (HashImport fs = HashImport.of("f")) {
      connection.executeCommand(commandObjects.himportSet("k1", fs, "v1"));
      connection.executeCommand(commandObjects.himportSet("k2", fs, "v2"));

      assertEquals(Arrays.asList( //
        "HIMPORT PREPARE " + fs.name() + " f", //
        "HIMPORT SET k1 " + fs.name() + " v1", //
        "HIMPORT SET k2 " + fs.name() + " v2"), received);
    }
  }

  @Test
  public void discardsClosedTemplateBeforeNextCommand() {
    HashImport fs = HashImport.of("f");
    connection.executeCommand(commandObjects.himportSet("k1", fs, "v1"));

    fs.close();
    // nothing is sent by close() itself
    assertEquals(2, received.size());

    connection.executeCommand(Protocol.Command.PING);

    assertEquals(Arrays.asList( //
      "HIMPORT PREPARE " + fs.name() + " f", //
      "HIMPORT SET k1 " + fs.name() + " v1", //
      "HIMPORT DISCARD " + fs.name(), //
      "PING"), received);
  }

  @Test
  public void resetClearsPreparedStateSoNextSetRePrepares() {
    try (HashImport fs = HashImport.of("f"); Jedis jedis = new Jedis(connection)) {
      connection.executeCommand(commandObjects.himportSet("k1", fs, "v1"));

      // RESET drops connection-scoped fieldsets server-side; the client note must follow
      jedis.reset();

      connection.executeCommand(commandObjects.himportSet("k2", fs, "v2"));
      assertEquals(Arrays.asList( //
        "HIMPORT PREPARE " + fs.name() + " f", //
        "HIMPORT SET k1 " + fs.name() + " v1", //
        "RESET", //
        "HIMPORT PREPARE " + fs.name() + " f", //
        "HIMPORT SET k2 " + fs.name() + " v2"), received);
    }
  }

  @Test
  public void batchesAllQueuedDiscardsBeforeOneCommand() {
    HashImport fs1 = HashImport.of("a");
    HashImport fs2 = HashImport.of("b");
    connection.executeCommand(commandObjects.himportSet("k1", fs1, "1"));
    connection.executeCommand(commandObjects.himportSet("k2", fs2, "2"));

    fs1.close();
    fs2.close();
    received.clear();

    connection.executeCommand(Protocol.Command.PING);

    // both DISCARDs drain ahead of the one command
    assertEquals("PING", received.get(received.size() - 1));
    assertEquals(3, received.size());
    assertEquals(
      new java.util.HashSet<>(
          Arrays.asList("HIMPORT DISCARD " + fs1.name(), "HIMPORT DISCARD " + fs2.name())),
      new java.util.HashSet<>(received.subList(0, 2)));
  }
}
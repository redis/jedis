package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.RedisInputStream;

public class ConnectionErrorHandlingTest {

  private static final byte[] PING_COMMAND = "*1\r\n$4\r\nPING\r\n"
      .getBytes(StandardCharsets.UTF_8);

  @Test
  public void errorDuringCommandSerializationMarksConnectionBrokenAndRethrowsSameError() {
    SyntheticError expected = new SyntheticError();
    try (Connection conn = new Connection(
        fakeSocketFactory(new byte[0], new ByteArrayOutputStream()))) {
      CommandArguments args = new CommandArguments(Command.SET).add(new ErrorRawable(expected));

      SyntheticError thrown = assertThrows(SyntheticError.class, () -> conn.sendCommand(args));

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void runtimeExceptionDuringCommandSerializationMarksConnectionBroken() {
    RuntimeException expected = new IllegalStateException("raw argument failed");
    try (Connection conn = new Connection(
        fakeSocketFactory(new byte[0], new ByteArrayOutputStream()))) {
      CommandArguments args = new CommandArguments(Command.SET).add(new RuntimeRawable(expected));

      RuntimeException thrown = assertThrows(RuntimeException.class, () -> conn.sendCommand(args));

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void nullRawCommandArgumentMarksConnectionBroken() {
    try (Connection conn = new Connection(
        fakeSocketFactory(new byte[0], new ByteArrayOutputStream()))) {
      CommandArguments args = new CommandArguments(Command.SET).add(new NullRawable());

      assertThrows(NullPointerException.class, () -> conn.sendCommand(args));

      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void jedisConnectionExceptionDuringDirectConnectMarksConnectionBrokenAndRethrowsSameException() {
    JedisConnectionException expected = new JedisConnectionException("connect failed");
    try (Connection conn = new Connection(() -> {
      throw expected;
    })) {

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::connect);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void runtimeExceptionDuringDirectConnectMarksConnectionBrokenAndRethrowsSameException() {
    RuntimeException expected = new IllegalStateException("connect failed");
    try (Connection conn = new Connection(() -> {
      throw expected;
    })) {

      RuntimeException thrown = assertThrows(RuntimeException.class, conn::connect);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void errorDuringDirectConnectMarksConnectionBrokenAndRethrowsSameError() {
    SyntheticError expected = new SyntheticError();
    try (Connection conn = new Connection(() -> {
      throw expected;
    })) {

      SyntheticError thrown = assertThrows(SyntheticError.class, conn::connect);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void runtimeExceptionDuringSendCommandConnectMarksConnectionBrokenAndRethrowsSameException() {
    RuntimeException expected = new IllegalStateException("connect failed");
    try (Connection conn = new Connection(() -> {
      throw expected;
    })) {
      CommandArguments args = new CommandArguments(Command.PING);

      RuntimeException thrown = assertThrows(RuntimeException.class, () -> conn.sendCommand(args));

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void errorDuringSendCommandConnectMarksConnectionBrokenAndRethrowsSameError() {
    SyntheticError expected = new SyntheticError();
    try (Connection conn = new Connection(() -> {
      throw expected;
    })) {
      CommandArguments args = new CommandArguments(Command.PING);

      SyntheticError thrown = assertThrows(SyntheticError.class, () -> conn.sendCommand(args));

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void socketExceptionDuringSoTimeoutInitializationMarksConnectionBrokenAndClosesSocket() {
    SocketException expected = new SocketException("timeout init failed");
    CloseTrackingFakeSocket socket = new SocketExceptionOnGetSoTimeoutSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::connect);

      assertSame(expected, thrown.getCause());
      assertTrue(conn.isBroken());
      assertTrue(socket.isClosed());
    }
  }

  @Test
  public void runtimeExceptionDuringSoTimeoutInitializationMarksConnectionBrokenAndClosesSocket() {
    RuntimeException expected = new IllegalStateException("timeout init failed");
    CloseTrackingFakeSocket socket = new RuntimeExceptionOnGetSoTimeoutSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {

      RuntimeException thrown = assertThrows(RuntimeException.class, conn::connect);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
      assertTrue(socket.isClosed());
    }
  }

  @Test
  public void errorDuringSoTimeoutInitializationMarksConnectionBrokenAndClosesSocket() {
    SyntheticError expected = new SyntheticError();
    CloseTrackingFakeSocket socket = new ErrorOnGetSoTimeoutSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {

      SyntheticError thrown = assertThrows(SyntheticError.class, conn::connect);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
      assertTrue(socket.isClosed());
    }
  }

  @Test
  public void ioExceptionDuringOutputStreamInitializationMarksConnectionBrokenAndClosesSocket() {
    IOException expected = new IOException("output init failed");
    CloseTrackingFakeSocket socket = new IOExceptionOnGetOutputStreamSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::connect);

      assertSame(expected, thrown.getCause());
      assertTrue(conn.isBroken());
      assertTrue(socket.isClosed());
    }
  }

  @Test
  public void runtimeExceptionDuringOutputStreamInitializationMarksConnectionBrokenAndClosesSocket() {
    RuntimeException expected = new IllegalStateException("output init failed");
    CloseTrackingFakeSocket socket = new RuntimeExceptionOnGetOutputStreamSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {

      RuntimeException thrown = assertThrows(RuntimeException.class, conn::connect);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
      assertTrue(socket.isClosed());
    }
  }

  @Test
  public void errorDuringOutputStreamInitializationMarksConnectionBrokenAndClosesSocket() {
    SyntheticError expected = new SyntheticError();
    CloseTrackingFakeSocket socket = new ErrorOnGetOutputStreamSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {

      SyntheticError thrown = assertThrows(SyntheticError.class, conn::connect);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
      assertTrue(socket.isClosed());
    }
  }

  @Test
  public void ioExceptionDuringInputStreamInitializationMarksConnectionBrokenAndClosesSocket() {
    IOException expected = new IOException("input init failed");
    CloseTrackingFakeSocket socket = new IOExceptionOnGetInputStreamSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::connect);

      assertSame(expected, thrown.getCause());
      assertTrue(conn.isBroken());
      assertTrue(socket.isClosed());
    }
  }

  @Test
  public void runtimeExceptionDuringInputStreamInitializationMarksConnectionBrokenAndClosesSocket() {
    RuntimeException expected = new IllegalStateException("input init failed");
    CloseTrackingFakeSocket socket = new RuntimeExceptionOnGetInputStreamSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {

      RuntimeException thrown = assertThrows(RuntimeException.class, conn::connect);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
      assertTrue(socket.isClosed());
    }
  }

  @Test
  public void errorDuringInputStreamInitializationMarksConnectionBrokenAndClosesSocket() {
    SyntheticError expected = new SyntheticError();
    CloseTrackingFakeSocket socket = new ErrorOnGetInputStreamSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {

      SyntheticError thrown = assertThrows(SyntheticError.class, conn::connect);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
      assertTrue(socket.isClosed());
    }
  }

  @Test
  public void socketExceptionDuringSetSoTimeoutMarksConnectionBrokenAndWrapsCause() {
    SocketException expected = new SocketException("set timeout failed");
    CloseTrackingFakeSocket socket = new SocketExceptionOnSetSoTimeoutSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        () -> conn.setSoTimeout(500));

      assertSame(expected, thrown.getCause());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void socketExceptionDuringSetTimeoutInfiniteMarksConnectionBrokenAndWrapsCause() {
    SocketException expected = new SocketException("set infinite timeout failed");
    CloseTrackingFakeSocket socket = new SocketExceptionOnSetSoTimeoutSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        conn::setTimeoutInfinite);

      assertSame(expected, thrown.getCause());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void socketExceptionDuringRollbackTimeoutMarksConnectionBrokenAndWrapsCause() {
    SocketException expected = new SocketException("rollback timeout failed");
    CloseTrackingFakeSocket socket = new SocketExceptionOnSetSoTimeoutSocket(expected);
    try (Connection conn = new Connection(() -> socket)) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        conn::rollbackTimeout);

      assertSame(expected, thrown.getCause());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void alreadyBrokenConnectedConnectionRejectsCommandArgumentsWithoutWriting() {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (Connection conn = new Connection(fakeSocketFactory(new byte[0], output))) {
      conn.connect();
      conn.setBroken();
      CommandArguments args = new CommandArguments(Command.SET).add("key").add(new byte[9000]);

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        () -> conn.sendCommand(args));

      assertEquals("Attempting to write to a broken connection.", thrown.getMessage());
      assertEquals(0, output.size());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void alreadyBrokenConnectedConnectionRejectsBeforeArgumentSerialization() {
    SyntheticError unexpected = new SyntheticError();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (Connection conn = new Connection(fakeSocketFactory(new byte[0], output))) {
      conn.connect();
      conn.setBroken();
      CommandArguments args = new CommandArguments(Command.SET).add(new ErrorRawable(unexpected));

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        () -> conn.sendCommand(args));

      assertEquals("Attempting to write to a broken connection.", thrown.getMessage());
      assertEquals(0, output.size());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void alreadyBrokenConnectedConnectionDoesNotAttemptDiagnosticRedisErrorRead() {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (Connection conn = new Connection(fakeSocketFactory(
      "-ERR should not be read\r\n".getBytes(StandardCharsets.UTF_8), output))) {
      conn.connect();
      conn.setBroken();
      CommandArguments args = new CommandArguments(Command.SET).add("key").add(new byte[9000]);

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        () -> conn.sendCommand(args));

      assertEquals("Attempting to write to a broken connection.", thrown.getMessage());
      assertNull(thrown.getCause());
      assertEquals(0, output.size());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void brokenDisconnectedConnectionReconnectsBeforeApplyingWriteGuard() {
    AtomicInteger sockets = new AtomicInteger();
    ByteArrayOutputStream firstOutput = new ByteArrayOutputStream();
    ByteArrayOutputStream replacementOutput = new ByteArrayOutputStream();
    try (Connection conn = new Connection(() -> sockets.getAndIncrement() == 0
        ? new CloseTrackingFakeSocket(new ByteArrayInputStream(new byte[0]), firstOutput)
        : new CloseTrackingFakeSocket(new ByteArrayInputStream(new byte[0]), replacementOutput))) {
      conn.connect();
      conn.setBroken();
      conn.disconnect();
      CommandArguments args = new CommandArguments(Command.SET).add("key").add(new byte[9000]);

      conn.sendCommand(args);

      assertFalse(conn.isBroken());
      assertEquals(0, firstOutput.size());
      assertTrue(replacementOutput.size() > 0);
      assertEquals(2, sockets.get());
    }
  }

  @Test
  @SuppressWarnings("resource")
  public void errorDuringCommandOutputWriteMarksConnectionBrokenAndRethrowsSameError() {
    SyntheticError expected = new SyntheticError();
    Connection conn = new Connection(
        fakeSocketFactory(new byte[0], new ErrorOnWriteOutputStream(expected)));
    CommandArguments args = new CommandArguments(Command.SET).add(new byte[9000]);

    SyntheticError thrown = assertThrows(SyntheticError.class, () -> conn.sendCommand(args));

    assertSame(expected, thrown);
    assertTrue(conn.isBroken());
  }

  @Test
  @SuppressWarnings("resource")
  public void commandWriteFailureReadsRedisErrorLineAndStillMarksConnectionBroken() {
    IOException expected = new IOException("write failed");
    Connection conn = new Connection(
        fakeSocketFactory("-ERR invalid request\r\n".getBytes(StandardCharsets.UTF_8),
          new IOExceptionOnWriteOutputStream(expected)));
    CommandArguments args = new CommandArguments(Command.SET).add(new byte[9000]);

    JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
      () -> conn.sendCommand(args));

    assertEquals("ERR invalid request", thrown.getMessage());
    assertSame(expected, thrown.getCause());
    assertTrue(conn.isBroken());
  }

  @Test
  @SuppressWarnings("resource")
  public void commandWriteFailureIgnoresNonErrorDiagnosticReplyAndPreservesCause() {
    IOException expected = new IOException("write failed");
    Connection conn = new Connection(fakeSocketFactory("+OK\r\n".getBytes(StandardCharsets.UTF_8),
      new IOExceptionOnWriteOutputStream(expected)));
    CommandArguments args = new CommandArguments(Command.SET).add(new byte[9000]);

    JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
      () -> conn.sendCommand(args));

    assertSame(expected, thrown.getCause());
    assertTrue(conn.isBroken());
  }

  @Test
  @SuppressWarnings("resource")
  public void commandWriteFailureSwallowsDiagnosticReadFailureAndPreservesCause() {
    IOException expected = new IOException("write failed");
    IOException diagnosticFailure = new IOException("diagnostic read failed");
    Connection conn = new Connection(
        () -> new FakeSocket(new IOExceptionOnReadInputStream(diagnosticFailure),
            new IOExceptionOnWriteOutputStream(expected)));
    CommandArguments args = new CommandArguments(Command.SET).add(new byte[9000]);

    JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
      () -> conn.sendCommand(args));

    assertSame(expected, thrown.getCause());
    assertTrue(conn.isBroken());
  }

  @Test
  @SuppressWarnings("resource")
  public void commandWriteFailureMarksConnectionBrokenWhenDiagnosticReadThrowsError() {
    IOException expected = new IOException("write failed");
    SyntheticError diagnosticFailure = new SyntheticError();
    Connection conn = new Connection(
        () -> new FakeSocket(new ErrorOnReadInputStream(diagnosticFailure),
            new IOExceptionOnWriteOutputStream(expected)));
    CommandArguments args = new CommandArguments(Command.SET).add(new byte[9000]);

    SyntheticError thrown = assertThrows(SyntheticError.class, () -> conn.sendCommand(args));

    assertSame(diagnosticFailure, thrown);
    assertTrue(conn.isBroken());
  }

  @Test
  public void executeCommandMarksConnectionBrokenWhenReplyReadFailsAfterCommandIsFlushed() {
    SyntheticError expected = new SyntheticError();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (Connection conn = new ProtocolReadErrorConnection(expected,
        fakeSocketFactory(new byte[0], output))) {

      SyntheticError thrown = assertThrows(SyntheticError.class,
        () -> conn.executeCommand(Command.PING));

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
      assertArrayEquals(PING_COMMAND, output.toByteArray());
    }
  }

  @Test
  public void commandObjectBuilderLookupErrorAfterSuccessfulReadDoesNotMarkConnectionBroken() {
    SyntheticError expected = new SyntheticError();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (Connection conn = new Connection(
        fakeSocketFactory("+OK\r\n+NEXT\r\n".getBytes(StandardCharsets.UTF_8), output))) {
      CommandObject<Object> commandObject = new ErrorOnGetBuilderCommandObject(
          new CommandArguments(Command.PING), expected);

      SyntheticError thrown = assertThrows(SyntheticError.class,
        () -> conn.executeCommand(commandObject));

      assertSame(expected, thrown);
      assertFalse(conn.isBroken());
      assertArrayEquals(PING_COMMAND, output.toByteArray());
      assertArrayEquals("NEXT".getBytes(StandardCharsets.UTF_8), (byte[]) conn.getOne());
    }
  }

  @Test
  public void commandObjectBuilderErrorAfterSuccessfulReadDoesNotMarkConnectionBroken() {
    SyntheticError expected = new SyntheticError();
    try (Connection conn = new Connection(fakeSocketFactory(
      "+OK\r\n".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      CommandObject<String> commandObject = new CommandObject<>(new CommandArguments(Command.PING),
          new ErrorBuilder<>(expected));

      SyntheticError thrown = assertThrows(SyntheticError.class,
        () -> conn.executeCommand(commandObject));

      assertSame(expected, thrown);
      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void blockingCommandReadErrorMarksConnectionBroken() {
    SyntheticError expected = new SyntheticError();
    try (Connection conn = new ProtocolReadErrorConnection(expected,
        fakeSocketFactory(new byte[0], new ByteArrayOutputStream()))) {
      CommandObject<Object> commandObject = new CommandObject<>(
          new CommandArguments(Command.BLPOP).add("queue").add("0").blocking(),
          BuilderFactory.RAW_OBJECT);

      SyntheticError thrown = assertThrows(SyntheticError.class,
        () -> conn.executeCommand(commandObject));

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void blockingCommandBuilderErrorAfterSuccessfulReadDoesNotMarkConnectionBroken() {
    SyntheticError expected = new SyntheticError();
    try (Connection conn = new Connection(fakeSocketFactory(
      "+OK\r\n".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      CommandObject<String> commandObject = new CommandObject<>(
          new CommandArguments(Command.BLPOP).add("queue").add("0").blocking(),
          new ErrorBuilder<>(expected));

      SyntheticError thrown = assertThrows(SyntheticError.class,
        () -> conn.executeCommand(commandObject));

      assertSame(expected, thrown);
      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void malformedReplyDuringPublicReadMarksConnectionBroken() {
    try (Connection conn = new Connection(
        fakeSocketFactory("?\r\n".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::getOne);

      assertEquals("Unknown reply: ?", thrown.getMessage());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void truncatedBulkReplyDuringPublicReadMarksConnectionBroken() {
    try (Connection conn = new Connection(fakeSocketFactory(
      "$5\r\nabc".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::getOne);

      assertEquals("Unexpected end of stream.", thrown.getMessage());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void truncatedArrayReplyDuringPublicReadMarksConnectionBroken() {
    try (Connection conn = new Connection(fakeSocketFactory(
      "*2\r\n+OK\r\n".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::getOne);

      assertEquals("Unexpected end of stream.", thrown.getMessage());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void truncatedErrorReplyDuringPublicReadMarksConnectionBroken() {
    try (Connection conn = new Connection(fakeSocketFactory(
      "-ERR wrong type".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::getOne);

      assertEquals("Unexpected end of stream.", thrown.getMessage());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void ioExceptionDuringPublicReadMarksConnectionBrokenAndWrapsCause() {
    IOException expected = new IOException("read failed");
    try (Connection conn = new Connection(() -> new FakeSocket(
        new IOExceptionOnReadInputStream(expected), new ByteArrayOutputStream()))) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::getOne);

      assertSame(expected, thrown.getCause());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void invalidBooleanReplyDuringPublicReadMarksConnectionBroken() {
    try (Connection conn = new Connection(fakeSocketFactory(
      "#x\r\n".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::getOne);

      assertEquals("Unexpected character!", thrown.getMessage());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void invalidVerbatimStringLengthDuringPublicReadMarksConnectionBroken() {
    try (Connection conn = new Connection(fakeSocketFactory(
      "=3\r\ntxt\r\n".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::getOne);

      assertEquals("Bulk reply length 3 is less than expected 4", thrown.getMessage());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void nullBulkReplyDuringPublicReadDoesNotMarkConnectionBroken() {
    try (Connection conn = new Connection(fakeSocketFactory(
      "$-1\r\n+OK\r\n".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      conn.connect();

      assertNull(conn.getOne());

      assertFalse(conn.isBroken());
      assertArrayEquals("OK".getBytes(StandardCharsets.UTF_8), (byte[]) conn.getOne());
      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void nullArrayReplyDuringPublicReadDoesNotMarkConnectionBroken() {
    try (Connection conn = new Connection(fakeSocketFactory(
      "*-1\r\n+OK\r\n".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      conn.connect();

      assertNull(conn.getOne());

      assertFalse(conn.isBroken());
      assertArrayEquals("OK".getBytes(StandardCharsets.UTF_8), (byte[]) conn.getOne());
      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void redisErrorReplyDuringPublicReadDoesNotMarkConnectionBroken() {
    try (Connection conn = new Connection(
        fakeSocketFactory("-ERR wrong type\r\n+OK\r\n".getBytes(StandardCharsets.UTF_8),
          new ByteArrayOutputStream()))) {
      conn.connect();

      JedisDataException thrown = assertThrows(JedisDataException.class, conn::getOne);

      assertEquals("ERR wrong type", thrown.getMessage());
      assertFalse(conn.isBroken());
      assertArrayEquals("OK".getBytes(StandardCharsets.UTF_8), (byte[]) conn.getOne());
      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void redisErrorInsideArrayReplyIsReturnedAndDoesNotMarkConnectionBroken() {
    try (Connection conn = new Connection(
        fakeSocketFactory("*2\r\n-ERR wrong type\r\n+OK\r\n".getBytes(StandardCharsets.UTF_8),
          new ByteArrayOutputStream()))) {
      conn.connect();

      List<Object> replies = conn.getObjectMultiBulkReply();

      assertEquals(2, replies.size());
      assertInstanceOf(JedisDataException.class, replies.get(0));
      assertEquals("ERR wrong type", ((JedisDataException) replies.get(0)).getMessage());
      assertArrayEquals("OK".getBytes(StandardCharsets.UTF_8), (byte[]) replies.get(1));
      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void publicReplyHelpersMarkConnectionBrokenWhenProtocolReadFails() {
    List<ReadOperationCase> operations = Arrays.asList(
      new ReadOperationCase("getOne", Connection::getOne),
      new ReadOperationCase("getStatusCodeReply", Connection::getStatusCodeReply),
      new ReadOperationCase("getBulkReply", Connection::getBulkReply),
      new ReadOperationCase("getBinaryBulkReply", Connection::getBinaryBulkReply),
      new ReadOperationCase("getIntegerReply", Connection::getIntegerReply),
      new ReadOperationCase("getMultiBulkReply", Connection::getMultiBulkReply),
      new ReadOperationCase("getBinaryMultiBulkReply", Connection::getBinaryMultiBulkReply),
      new ReadOperationCase("getObjectMultiBulkReply", Connection::getObjectMultiBulkReply),
      new ReadOperationCase("getIntegerMultiBulkReply", Connection::getIntegerMultiBulkReply),
      new ReadOperationCase("getUnflushedObject", Connection::getUnflushedObject),
      new ReadOperationCase("getUnflushedObjectMultiBulkReply",
          ConnectionErrorHandlingTest::getUnflushedObjectMultiBulkReply));

    for (ReadOperationCase operation : operations) {
      SyntheticError expected = new SyntheticError();
      try (Connection conn = new ProtocolReadErrorConnection(expected,
          fakeSocketFactory(new byte[0], new ByteArrayOutputStream()))) {
        conn.connect();

        SyntheticError thrown = assertThrows(SyntheticError.class, () -> operation.run(conn),
          operation.name);

        assertSame(expected, thrown, operation.name);
        assertTrue(conn.isBroken(), operation.name);
      }
    }
  }

  @Test
  @SuppressWarnings("resource")
  public void publicReplyHelpersMarkConnectionBrokenWhenFlushFailsWithoutProtocolRead() {
    List<ReadOperationCase> operations = Arrays.asList(
      new ReadOperationCase("getOne", Connection::getOne),
      new ReadOperationCase("getStatusCodeReply", Connection::getStatusCodeReply),
      new ReadOperationCase("getBulkReply", Connection::getBulkReply),
      new ReadOperationCase("getBinaryBulkReply", Connection::getBinaryBulkReply),
      new ReadOperationCase("getIntegerReply", Connection::getIntegerReply),
      new ReadOperationCase("getMultiBulkReply", Connection::getMultiBulkReply),
      new ReadOperationCase("getBinaryMultiBulkReply", Connection::getBinaryMultiBulkReply),
      new ReadOperationCase("getObjectMultiBulkReply", Connection::getObjectMultiBulkReply),
      new ReadOperationCase("getIntegerMultiBulkReply", Connection::getIntegerMultiBulkReply),
      new ReadOperationCase("getMany", connection -> connection.getMany(2)));

    for (ReadOperationCase operation : operations) {
      SyntheticError expected = new SyntheticError();
      CountingSuccessfulReadConnection conn = new CountingSuccessfulReadConnection(
          fakeSocketFactory(new byte[0], new ErrorOnFlushOutputStream(expected)));
      conn.connect();

      SyntheticError thrown = assertThrows(SyntheticError.class, () -> operation.run(conn),
        operation.name);

      assertSame(expected, thrown, operation.name);
      assertEquals(0, conn.reads(), operation.name);
      assertTrue(conn.isBroken(), operation.name);
    }
  }

  @Test
  @SuppressWarnings("resource")
  public void errorDuringFlushMarksConnectionBrokenAndRethrowsSameError() {
    SyntheticError expected = new SyntheticError();
    Connection conn = new Connection(
        fakeSocketFactory(new byte[0], new ErrorOnFlushOutputStream(expected)));
    conn.connect();

    SyntheticError thrown = assertThrows(SyntheticError.class, conn::flush);

    assertSame(expected, thrown);
    assertTrue(conn.isBroken());
  }

  @Test
  @SuppressWarnings("resource")
  public void ioExceptionDuringFlushMarksConnectionBrokenAndWrapsCause() {
    IOException expected = new IOException("flush failed");
    Connection conn = new Connection(
        fakeSocketFactory(new byte[0], new IOExceptionOnFlushOutputStream(expected)));
    conn.connect();

    JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::flush);

    assertSame(expected, thrown.getCause());
    assertTrue(conn.isBroken());
  }

  @Test
  @SuppressWarnings("resource")
  public void closeRethrowsErrorWhenDisconnectFlushFailsOnBrokenConnection() {
    SyntheticError expected = new SyntheticError();
    Connection conn = new Connection(
        fakeSocketFactory(new byte[0], new ErrorOnFlushOutputStream(expected)));
    conn.connect();
    conn.setBroken();

    SyntheticError thrown = assertThrows(SyntheticError.class, conn::close);

    assertSame(expected, thrown);
  }

  @Test
  @SuppressWarnings("resource")
  public void closeWrapsIOExceptionWhenDisconnectFlushFailsOnBrokenConnection() {
    IOException expected = new IOException("flush failed");
    Connection conn = new Connection(
        fakeSocketFactory(new byte[0], new IOExceptionOnFlushOutputStream(expected)));
    conn.connect();
    conn.setBroken();

    JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::close);

    assertSame(expected, thrown.getCause());
  }

  @Test
  public void errorDuringProtocolReadMarksConnectionBrokenAndRethrowsSameError() {
    SyntheticError expected = new SyntheticError();
    try (Connection conn = new ProtocolReadErrorConnection(expected)) {

      SyntheticError thrown = assertThrows(SyntheticError.class,
        conn::readProtocolWithCheckingBroken);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void runtimeExceptionDuringProtocolReadMarksConnectionBroken() {
    RuntimeException expected = new IllegalStateException("decoder bug");
    try (Connection conn = new ProtocolReadRuntimeExceptionConnection(expected)) {
      RuntimeException thrown = assertThrows(RuntimeException.class,
        conn::readProtocolWithCheckingBroken);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void alreadyBrokenConnectionRejectsProtocolReadWithoutReadingAgain() {
    try (CountingSuccessfulReadConnection conn = new CountingSuccessfulReadConnection()) {
      conn.setBroken();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        conn::readProtocolWithCheckingBroken);

      assertEquals("Attempting to read from a broken connection.", thrown.getMessage());
      assertEquals(0, conn.reads());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  @SuppressWarnings("resource")
  public void alreadyBrokenPublicReplyHelperRejectsBeforeFlushOrRead() {
    SyntheticError unexpected = new SyntheticError();
    CountingSuccessfulReadConnection conn = new CountingSuccessfulReadConnection(
        fakeSocketFactory(new byte[0], new ErrorOnFlushOutputStream(unexpected)));
    conn.connect();
    conn.setBroken();

    JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::getOne);

    assertEquals("Attempting to write to a broken connection.", thrown.getMessage());
    assertEquals(0, conn.reads());
    assertTrue(conn.isBroken());
  }

  @Test
  public void errorDuringGetManyReadLoopMarksConnectionBrokenAndRethrowsSameError() {
    SyntheticError expected = new SyntheticError();
    try (Connection conn = new CountingReadConnection(
        fakeSocketFactory(new byte[0], new ByteArrayOutputStream()), expected)) {
      conn.connect();

      SyntheticError thrown = assertThrows(SyntheticError.class, () -> conn.getMany(2));

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  @SuppressWarnings("resource")
  public void errorDuringGetManyFlushPreventsAnyProtocolReadAndMarksConnectionBroken() {
    SyntheticError expected = new SyntheticError();
    CountingSuccessfulReadConnection conn = new CountingSuccessfulReadConnection(
        fakeSocketFactory(new byte[0], new ErrorOnFlushOutputStream(expected)));
    conn.connect();

    SyntheticError thrown = assertThrows(SyntheticError.class, () -> conn.getMany(2));

    assertSame(expected, thrown);
    assertEquals(0, conn.reads());
    assertTrue(conn.isBroken());
  }

  @Test
  public void jedisDataExceptionDuringGetManyIsReturnedAsReplyAndDoesNotBreakConnection() {
    JedisDataException expected = new JedisDataException("ERR wrong type");
    try (Connection conn = new DataExceptionThenValueConnection(
        fakeSocketFactory(new byte[0], new ByteArrayOutputStream()), expected)) {
      conn.connect();

      List<Object> replies = conn.getMany(2);

      assertEquals(2, replies.size());
      assertSame(expected, replies.get(0));
      assertEquals("OK", replies.get(1));
      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void jedisConnectionExceptionDuringGetManyReadLoopMarksConnectionBroken() {
    JedisConnectionException expected = new JedisConnectionException("read failed");
    try (Connection conn = new ConnectionExceptionThenValueConnection(
        fakeSocketFactory(new byte[0], new ByteArrayOutputStream()), expected)) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        () -> conn.getMany(2));

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void errorDuringPushReadMarksConnectionBrokenAndRethrowsSameError() {
    SyntheticError expected = new SyntheticError();
    try (Connection conn = new PushReadErrorConnection(expected)) {
      conn.connect();

      SyntheticError thrown = assertThrows(SyntheticError.class,
        conn::readPushesWithCheckingBroken);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void jedisConnectionExceptionDuringPushReadMarksConnectionBroken() {
    JedisConnectionException expected = new JedisConnectionException("push failed");
    try (Connection conn = new PushReadConnectionExceptionConnection(expected)) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        conn::readPushesWithCheckingBroken);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void runtimeExceptionDuringPushReadMarksConnectionBroken() {
    RuntimeException expected = new IllegalStateException("consumer bug");
    try (Connection conn = new PushReadRuntimeExceptionConnection(expected)) {
      conn.connect();

      RuntimeException thrown = assertThrows(RuntimeException.class,
        conn::readPushesWithCheckingBroken);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void errorDuringPushAvailabilityCheckMarksConnectionBrokenAndRethrowsSameError() {
    SyntheticError expected = new SyntheticError();
    try (Connection conn = new Connection(() -> new FakeSocket(
        new ErrorOnAvailableInputStream(expected), new ByteArrayOutputStream()))) {
      conn.connect();

      SyntheticError thrown = assertThrows(SyntheticError.class,
        conn::readPushesWithCheckingBroken);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void ioExceptionDuringPushAvailabilityCheckMarksConnectionBrokenAndWrapsCause() {
    IOException expected = new IOException("available failed");
    try (Connection conn = new Connection(() -> new FakeSocket(
        new IOExceptionOnAvailableInputStream(expected), new ByteArrayOutputStream()))) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        conn::readPushesWithCheckingBroken);

      assertSame(expected, thrown.getCause());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void readPushesDoesNothingWhenNoPushBytesAreBuffered() {
    try (CountingPushReadConnection conn = new CountingPushReadConnection(
        fakeSocketFactory(new byte[0], new ByteArrayOutputStream()))) {
      conn.connect();

      conn.readPushesWithCheckingBroken();

      assertEquals(0, conn.pushReads());
      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void successfulPushReadLeavesConnectionHealthyAndReadsOnePushBatch() {
    try (CountingPushReadConnection conn = new CountingPushReadConnection(
        fakeSocketFactory(new byte[] { 1 }, new ByteArrayOutputStream()))) {
      conn.connect();

      conn.readPushesWithCheckingBroken();

      assertEquals(1, conn.pushReads());
      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void malformedBufferedPushReplyMarksConnectionBroken() {
    try (Connection conn = new RealPushParsingConnection(
        fakeSocketFactory(">2\r\n$10\r\ninvalidate\r\n$3\r\nab".getBytes(StandardCharsets.UTF_8),
          new ByteArrayOutputStream()))) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        conn::readPushesWithCheckingBroken);

      assertEquals("Unexpected end of stream.", thrown.getMessage());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void malformedBufferedPushElementMarksConnectionBroken() {
    try (Connection conn = new RealPushParsingConnection(fakeSocketFactory(
      ">1\r\n?\r\n".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        conn::readPushesWithCheckingBroken);

      assertEquals("Unknown reply: ?", thrown.getMessage());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void redisErrorInsideBufferedPushReplyDoesNotMarkConnectionBroken() {
    try (Connection conn = new RealPushParsingConnection(fakeSocketFactory(
      ">2\r\n$9\r\narbitrary\r\n-ERR payload failed\r\n".getBytes(StandardCharsets.UTF_8),
      new ByteArrayOutputStream()))) {
      conn.connect();

      conn.readPushesWithCheckingBroken();

      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void bufferedNonPushReplyIsNotConsumedAndDoesNotBreakConnection() {
    try (Connection conn = new RealPushParsingConnection(fakeSocketFactory(
      "+OK\r\n".getBytes(StandardCharsets.UTF_8), new ByteArrayOutputStream()))) {
      conn.connect();

      conn.readPushesWithCheckingBroken();

      assertFalse(conn.isBroken());
      assertArrayEquals("OK".getBytes(StandardCharsets.UTF_8), (byte[]) conn.getOne());
      assertFalse(conn.isBroken());
    }
  }

  @Test
  public void alreadyBrokenConnectionRejectsPushReadWithoutCheckingAvailability() {
    SyntheticError unexpected = new SyntheticError();
    try (Connection conn = new Connection(() -> new FakeSocket(
        new ErrorOnAvailableInputStream(unexpected), new ByteArrayOutputStream()))) {
      conn.connect();
      conn.setBroken();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        conn::readPushesWithCheckingBroken);

      assertEquals("Attempting to read from a broken connection.", thrown.getMessage());
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void errorDuringProtocolReadCausesPooledConnectionToBeInvalidatedOnClose() {
    AtomicInteger destroyed = new AtomicInteger();
    SyntheticError expected = new SyntheticError();

    try (ConnectionPool pool = newSingleConnectionPool(destroyed,
      () -> new ProtocolReadErrorConnection(expected))) {
      Connection conn = pool.getResource();

      SyntheticError thrown = assertThrows(SyntheticError.class,
        conn::readProtocolWithCheckingBroken);
      conn.close();

      assertSame(expected, thrown);
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void errorDuringCommandSerializationCausesPooledConnectionToBeInvalidatedOnClose() {
    AtomicInteger destroyed = new AtomicInteger();
    SyntheticError expected = new SyntheticError();

    try (ConnectionPool pool = newSingleConnectionPool(destroyed,
      () -> new Connection(fakeSocketFactory(new byte[0], new ByteArrayOutputStream())))) {
      Connection conn = pool.getResource();
      CommandArguments args = new CommandArguments(Command.SET).add(new ErrorRawable(expected));

      SyntheticError thrown = assertThrows(SyntheticError.class, () -> conn.sendCommand(args));
      conn.close();

      assertSame(expected, thrown);
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void runtimeExceptionDuringCommandSerializationInvalidatesPooledConnection() {
    AtomicInteger destroyed = new AtomicInteger();
    RuntimeException expected = new IllegalStateException("raw argument failed");

    try (ConnectionPool pool = newSingleConnectionPool(destroyed,
      () -> new Connection(fakeSocketFactory(new byte[0], new ByteArrayOutputStream())))) {
      Connection conn = pool.getResource();
      CommandArguments args = new CommandArguments(Command.SET).add(new RuntimeRawable(expected));

      RuntimeException thrown = assertThrows(RuntimeException.class, () -> conn.sendCommand(args));
      conn.close();

      assertSame(expected, thrown);
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void commandWriteFailureInvalidatesPooledConnection() {
    AtomicInteger destroyed = new AtomicInteger();
    IOException expected = new IOException("write failed");

    try (ConnectionPool pool = newSingleConnectionPool(destroyed,
      () -> new Connection(
          fakeSocketFactory("-ERR invalid request\r\n".getBytes(StandardCharsets.UTF_8),
            new IOExceptionOnWriteOutputStream(expected))))) {
      Connection conn = pool.getResource();
      CommandArguments args = new CommandArguments(Command.SET).add(new byte[9000]);

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        () -> conn.sendCommand(args));
      conn.close();

      assertEquals("ERR invalid request", thrown.getMessage());
      assertSame(expected, thrown.getCause());
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void runtimeExceptionDuringConnectInvalidatesPooledConnectionOnClose() {
    AtomicInteger destroyed = new AtomicInteger();
    RuntimeException expected = new IllegalStateException("connect failed");

    try (ConnectionPool pool = newSingleConnectionPool(destroyed, () -> new Connection(() -> {
      throw expected;
    }))) {
      Connection conn = pool.getResource();
      CommandArguments args = new CommandArguments(Command.PING);

      RuntimeException thrown = assertThrows(RuntimeException.class, () -> conn.sendCommand(args));
      conn.close();

      assertSame(expected, thrown);
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void errorDuringConnectInvalidatesPooledConnectionOnClose() {
    AtomicInteger destroyed = new AtomicInteger();
    SyntheticError expected = new SyntheticError();

    try (ConnectionPool pool = newSingleConnectionPool(destroyed, () -> new Connection(() -> {
      throw expected;
    }))) {
      Connection conn = pool.getResource();
      CommandArguments args = new CommandArguments(Command.PING);

      SyntheticError thrown = assertThrows(SyntheticError.class, () -> conn.sendCommand(args));
      conn.close();

      assertSame(expected, thrown);
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void runtimeExceptionDuringDirectConnectInvalidatesPooledConnectionOnClose() {
    AtomicInteger destroyed = new AtomicInteger();
    RuntimeException expected = new IllegalStateException("connect failed");

    try (ConnectionPool pool = newSingleConnectionPool(destroyed, () -> new Connection(() -> {
      throw expected;
    }))) {
      Connection conn = pool.getResource();

      RuntimeException thrown = assertThrows(RuntimeException.class, conn::connect);
      conn.close();

      assertSame(expected, thrown);
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void errorDuringDirectConnectInvalidatesPooledConnectionOnClose() {
    AtomicInteger destroyed = new AtomicInteger();
    SyntheticError expected = new SyntheticError();

    try (ConnectionPool pool = newSingleConnectionPool(destroyed, () -> new Connection(() -> {
      throw expected;
    }))) {
      Connection conn = pool.getResource();

      SyntheticError thrown = assertThrows(SyntheticError.class, conn::connect);
      conn.close();

      assertSame(expected, thrown);
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void partialSocketConnectFailureInvalidatesPooledConnectionAndClosesSocket() {
    AtomicInteger destroyed = new AtomicInteger();
    RuntimeException expected = new IllegalStateException("input init failed");
    CloseTrackingFakeSocket socket = new RuntimeExceptionOnGetInputStreamSocket(expected);

    try (ConnectionPool pool = newSingleConnectionPool(destroyed,
      () -> new Connection(() -> socket))) {
      Connection conn = pool.getResource();

      RuntimeException thrown = assertThrows(RuntimeException.class, conn::connect);
      conn.close();

      assertSame(expected, thrown);
      assertTrue(socket.isClosed());
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void socketExceptionDuringTimeoutChangeInvalidatesPooledConnectionOnClose() {
    AtomicInteger destroyed = new AtomicInteger();
    SocketException expected = new SocketException("set timeout failed");

    try (ConnectionPool pool = newSingleConnectionPool(destroyed,
      () -> new Connection(() -> new SocketExceptionOnSetSoTimeoutSocket(expected)))) {
      Connection conn = pool.getResource();
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        () -> conn.setSoTimeout(500));
      conn.close();

      assertSame(expected, thrown.getCause());
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void alreadyBrokenPooledConnectionInvalidatesOnCloseWithoutWriting() {
    AtomicInteger destroyed = new AtomicInteger();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    try (ConnectionPool pool = newSingleConnectionPool(destroyed,
      () -> new Connection(fakeSocketFactory(new byte[0], output)))) {
      Connection conn = pool.getResource();
      conn.connect();
      conn.setBroken();
      CommandArguments args = new CommandArguments(Command.SET).add("key").add(new byte[9000]);

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        () -> conn.sendCommand(args));
      conn.close();

      assertEquals("Attempting to write to a broken connection.", thrown.getMessage());
      assertEquals(0, output.size());
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void errorDuringFlushCausesPooledConnectionToBeInvalidatedOnClose() {
    AtomicInteger destroyed = new AtomicInteger();
    SyntheticError expected = new SyntheticError();

    try (ConnectionPool pool = newSingleConnectionPool(destroyed, () -> new Connection(
        fakeSocketFactory(new byte[0], new ErrorOnFlushOutputStream(expected))))) {
      Connection conn = pool.getResource();
      conn.connect();

      SyntheticError thrown = assertThrows(SyntheticError.class, conn::flush);
      conn.close();

      assertSame(expected, thrown);
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void errorDuringPushReadCausesPooledConnectionToBeInvalidatedOnClose() {
    AtomicInteger destroyed = new AtomicInteger();
    SyntheticError expected = new SyntheticError();

    try (ConnectionPool pool = newSingleConnectionPool(destroyed,
      () -> new PushReadErrorConnection(expected))) {
      Connection conn = pool.getResource();
      conn.connect();

      SyntheticError thrown = assertThrows(SyntheticError.class,
        conn::readPushesWithCheckingBroken);
      conn.close();

      assertSame(expected, thrown);
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void malformedReplyDuringPublicReadInvalidatesPooledConnection() {
    AtomicInteger destroyed = new AtomicInteger();

    try (ConnectionPool pool = newSingleConnectionPool(destroyed,
      () -> new Connection(fakeSocketFactory("$5\r\nabc".getBytes(StandardCharsets.UTF_8),
        new ByteArrayOutputStream())))) {
      Connection conn = pool.getResource();
      conn.connect();

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class, conn::getOne);
      conn.close();

      assertEquals("Unexpected end of stream.", thrown.getMessage());
      assertEquals(1, destroyed.get());
      Connection replacement = pool.getResource();
      assertNotSame(conn, replacement);
      assertFalse(replacement.isBroken());
      replacement.close();
    }
  }

  @Test
  public void redisDataExceptionDoesNotInvalidatePooledConnectionOnClose() {
    AtomicInteger destroyed = new AtomicInteger();

    try (ConnectionPool pool = newSingleConnectionPool(destroyed,
      () -> new Connection(fakeSocketFactory("-ERR wrong type\r\n".getBytes(StandardCharsets.UTF_8),
        new ByteArrayOutputStream())))) {
      Connection conn = pool.getResource();
      conn.connect();

      JedisDataException thrown = assertThrows(JedisDataException.class, conn::getOne);
      conn.close();

      assertEquals("ERR wrong type", thrown.getMessage());
      assertEquals(0, destroyed.get());
      Connection sameConnection = pool.getResource();
      assertSame(conn, sameConnection);
      assertFalse(sameConnection.isBroken());
      sameConnection.close();
    }
  }

  @Test
  public void jedisConnectionExceptionDuringProtocolReadMarksConnectionBroken() {
    JedisConnectionException expected = new JedisConnectionException("boom");
    try (Connection conn = new ProtocolReadConnectionExceptionConnection(expected)) {

      JedisConnectionException thrown = assertThrows(JedisConnectionException.class,
        conn::readProtocolWithCheckingBroken);

      assertSame(expected, thrown);
      assertTrue(conn.isBroken());
    }
  }

  @Test
  public void jedisDataExceptionDuringProtocolReadDoesNotMarkConnectionBroken() {
    JedisDataException expected = new JedisDataException("ERR wrong type");
    try (Connection conn = new ProtocolReadDataExceptionConnection(expected)) {
      JedisDataException thrown = assertThrows(JedisDataException.class,
        conn::readProtocolWithCheckingBroken);

      assertSame(expected, thrown);
      assertFalse(conn.isBroken());
    }
  }

  @SuppressWarnings("deprecation")
  private static Object getUnflushedObjectMultiBulkReply(Connection connection) {
    return connection.getUnflushedObjectMultiBulkReply();
  }

  private static JedisSocketFactory fakeSocketFactory(byte[] input, OutputStream output) {
    return () -> new FakeSocket(new ByteArrayInputStream(input), output);
  }

  private static ConnectionPool newSingleConnectionPool(AtomicInteger destroyed,
      Supplier<Connection> connectionSupplier) {
    GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    return new ConnectionPool(new SuppliedConnectionFactory(connectionSupplier, destroyed), config);
  }

  private interface ReadOperation {
    Object run(Connection connection);
  }

  private static final class ReadOperationCase {
    private final String name;
    private final ReadOperation operation;

    private ReadOperationCase(String name, ReadOperation operation) {
      this.name = name;
      this.operation = operation;
    }

    private Object run(Connection connection) {
      return operation.run(connection);
    }
  }

  private static class FakeSocket extends Socket {

    private final InputStream input;
    private final OutputStream output;

    FakeSocket(InputStream input, OutputStream output) {
      this.input = input;
      this.output = output;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return input;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      return output;
    }

    @Override
    public boolean isBound() {
      return true;
    }

    @Override
    public boolean isClosed() {
      return false;
    }

    @Override
    public boolean isConnected() {
      return true;
    }

    @Override
    public boolean isInputShutdown() {
      return false;
    }

    @Override
    public boolean isOutputShutdown() {
      return false;
    }

    @Override
    public int getSoTimeout() throws SocketException {
      return 0;
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
    }
  }

  private static class CloseTrackingFakeSocket extends FakeSocket {

    private boolean closed;

    private CloseTrackingFakeSocket(InputStream input, OutputStream output) {
      super(input, output);
    }

    @Override
    public boolean isClosed() {
      return closed;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  private static final class SocketExceptionOnGetSoTimeoutSocket extends CloseTrackingFakeSocket {

    private final SocketException exception;

    private SocketExceptionOnGetSoTimeoutSocket(SocketException exception) {
      super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
      this.exception = exception;
    }

    @Override
    public int getSoTimeout() throws SocketException {
      throw exception;
    }
  }

  private static final class RuntimeExceptionOnGetSoTimeoutSocket extends CloseTrackingFakeSocket {

    private final RuntimeException exception;

    private RuntimeExceptionOnGetSoTimeoutSocket(RuntimeException exception) {
      super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
      this.exception = exception;
    }

    @Override
    public int getSoTimeout() {
      throw exception;
    }
  }

  private static final class ErrorOnGetSoTimeoutSocket extends CloseTrackingFakeSocket {

    private final Error error;

    private ErrorOnGetSoTimeoutSocket(Error error) {
      super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
      this.error = error;
    }

    @Override
    public int getSoTimeout() {
      throw error;
    }
  }

  private static final class IOExceptionOnGetOutputStreamSocket extends CloseTrackingFakeSocket {

    private final IOException exception;

    private IOExceptionOnGetOutputStreamSocket(IOException exception) {
      super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
      this.exception = exception;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      throw exception;
    }
  }

  private static final class RuntimeExceptionOnGetOutputStreamSocket
      extends CloseTrackingFakeSocket {

    private final RuntimeException exception;

    private RuntimeExceptionOnGetOutputStreamSocket(RuntimeException exception) {
      super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
      this.exception = exception;
    }

    @Override
    public OutputStream getOutputStream() {
      throw exception;
    }
  }

  private static final class ErrorOnGetOutputStreamSocket extends CloseTrackingFakeSocket {

    private final Error error;

    private ErrorOnGetOutputStreamSocket(Error error) {
      super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
      this.error = error;
    }

    @Override
    public OutputStream getOutputStream() {
      throw error;
    }
  }

  private static final class IOExceptionOnGetInputStreamSocket extends CloseTrackingFakeSocket {

    private final IOException exception;

    private IOExceptionOnGetInputStreamSocket(IOException exception) {
      super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
      this.exception = exception;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      throw exception;
    }
  }

  private static final class RuntimeExceptionOnGetInputStreamSocket
      extends CloseTrackingFakeSocket {

    private final RuntimeException exception;

    private RuntimeExceptionOnGetInputStreamSocket(RuntimeException exception) {
      super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
      this.exception = exception;
    }

    @Override
    public InputStream getInputStream() {
      throw exception;
    }
  }

  private static final class ErrorOnGetInputStreamSocket extends CloseTrackingFakeSocket {

    private final Error error;

    private ErrorOnGetInputStreamSocket(Error error) {
      super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
      this.error = error;
    }

    @Override
    public InputStream getInputStream() {
      throw error;
    }
  }

  private static final class SocketExceptionOnSetSoTimeoutSocket extends CloseTrackingFakeSocket {

    private final SocketException exception;

    private SocketExceptionOnSetSoTimeoutSocket(SocketException exception) {
      super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
      this.exception = exception;
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
      throw exception;
    }
  }

  private static final class ErrorRawable implements Rawable {

    private final Error error;

    private ErrorRawable(Error error) {
      this.error = error;
    }

    @Override
    public byte[] getRaw() {
      throw error;
    }
  }

  private static final class RuntimeRawable implements Rawable {

    private final RuntimeException exception;

    private RuntimeRawable(RuntimeException exception) {
      this.exception = exception;
    }

    @Override
    public byte[] getRaw() {
      throw exception;
    }
  }

  private static final class NullRawable implements Rawable {

    @Override
    public byte[] getRaw() {
      return null;
    }
  }

  private static final class ErrorOnFlushOutputStream extends ByteArrayOutputStream {

    private final Error error;

    private ErrorOnFlushOutputStream(Error error) {
      this.error = error;
    }

    @Override
    public void flush() {
      throw error;
    }
  }

  private static final class ErrorOnWriteOutputStream extends OutputStream {

    private final Error error;

    private ErrorOnWriteOutputStream(Error error) {
      this.error = error;
    }

    @Override
    public void write(int b) {
      throw error;
    }

    @Override
    public void write(byte[] b, int off, int len) {
      throw error;
    }
  }

  private static final class IOExceptionOnFlushOutputStream extends ByteArrayOutputStream {

    private final IOException exception;

    private IOExceptionOnFlushOutputStream(IOException exception) {
      this.exception = exception;
    }

    @Override
    public void flush() throws IOException {
      throw exception;
    }
  }

  private static final class IOExceptionOnWriteOutputStream extends OutputStream {

    private final IOException exception;

    private IOExceptionOnWriteOutputStream(IOException exception) {
      this.exception = exception;
    }

    @Override
    public void write(int b) throws IOException {
      throw exception;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      throw exception;
    }
  }

  private static final class ProtocolReadErrorConnection extends Connection {

    private final Error error;

    private ProtocolReadErrorConnection(Error error) {
      this.error = error;
    }

    private ProtocolReadErrorConnection(Error error, JedisSocketFactory socketFactory) {
      super(socketFactory);
      this.error = error;
    }

    @Override
    protected Object protocolRead(RedisInputStream is, PushConsumerChain consumer) {
      throw error;
    }
  }

  private static final class ProtocolReadRuntimeExceptionConnection extends Connection {

    private final RuntimeException exception;

    private ProtocolReadRuntimeExceptionConnection(RuntimeException exception) {
      this.exception = exception;
    }

    @Override
    protected Object protocolRead(RedisInputStream is, PushConsumerChain consumer) {
      throw exception;
    }
  }

  private static final class CountingSuccessfulReadConnection extends Connection {

    private int reads;

    private CountingSuccessfulReadConnection() {
    }

    private CountingSuccessfulReadConnection(JedisSocketFactory socketFactory) {
      super(socketFactory);
    }

    @Override
    protected Object protocolRead(RedisInputStream is, PushConsumerChain consumer) {
      reads++;
      return "OK";
    }

    private int reads() {
      return reads;
    }
  }

  private static final class CountingReadConnection extends Connection {

    private final Error error;
    private int reads;

    private CountingReadConnection(JedisSocketFactory socketFactory, Error error) {
      super(socketFactory);
      this.error = error;
    }

    @Override
    protected Object protocolRead(RedisInputStream is, PushConsumerChain consumer) {
      if (++reads == 2) {
        throw error;
      }
      return "first";
    }
  }

  private static final class DataExceptionThenValueConnection extends Connection {

    private final JedisDataException exception;
    private int reads;

    private DataExceptionThenValueConnection(JedisSocketFactory socketFactory,
        JedisDataException exception) {
      super(socketFactory);
      this.exception = exception;
    }

    @Override
    protected Object protocolRead(RedisInputStream is, PushConsumerChain consumer) {
      if (++reads == 1) {
        throw exception;
      }
      return "OK";
    }
  }

  private static final class ConnectionExceptionThenValueConnection extends Connection {

    private final JedisConnectionException exception;
    private int reads;

    private ConnectionExceptionThenValueConnection(JedisSocketFactory socketFactory,
        JedisConnectionException exception) {
      super(socketFactory);
      this.exception = exception;
    }

    @Override
    protected Object protocolRead(RedisInputStream is, PushConsumerChain consumer) {
      if (++reads == 2) {
        throw exception;
      }
      return "first";
    }
  }

  private static final class PushReadErrorConnection extends Connection {

    private final Error error;

    private PushReadErrorConnection(Error error) {
      super(fakeSocketFactory(new byte[] { 1 }, new ByteArrayOutputStream()));
      this.error = error;
    }

    @Override
    protected void protocolReadPushes(RedisInputStream is, PushConsumerChain consumer) {
      throw error;
    }
  }

  private static final class PushReadConnectionExceptionConnection extends Connection {

    private final JedisConnectionException exception;

    private PushReadConnectionExceptionConnection(JedisConnectionException exception) {
      super(fakeSocketFactory(new byte[] { 1 }, new ByteArrayOutputStream()));
      this.exception = exception;
    }

    @Override
    protected void protocolReadPushes(RedisInputStream is, PushConsumerChain consumer) {
      throw exception;
    }
  }

  private static final class PushReadRuntimeExceptionConnection extends Connection {

    private final RuntimeException exception;

    private PushReadRuntimeExceptionConnection(RuntimeException exception) {
      super(fakeSocketFactory(new byte[] { 1 }, new ByteArrayOutputStream()));
      this.exception = exception;
    }

    @Override
    protected void protocolReadPushes(RedisInputStream is, PushConsumerChain consumer) {
      throw exception;
    }
  }

  private static final class CountingPushReadConnection extends Connection {

    private int pushReads;

    private CountingPushReadConnection(JedisSocketFactory socketFactory) {
      super(socketFactory);
    }

    @Override
    protected void protocolReadPushes(RedisInputStream is, PushConsumerChain consumer) {
      pushReads++;
    }

    private int pushReads() {
      return pushReads;
    }
  }

  private static final class RealPushParsingConnection extends Connection {

    private RealPushParsingConnection(JedisSocketFactory socketFactory) {
      super(socketFactory);
    }

    @Override
    protected void protocolReadPushes(RedisInputStream is, PushConsumerChain consumer) {
      Protocol.readPushes(is, consumer);
    }
  }

  private static final class ErrorOnAvailableInputStream extends InputStream {

    private final Error error;

    private ErrorOnAvailableInputStream(Error error) {
      this.error = error;
    }

    @Override
    public int read() {
      return -1;
    }

    @Override
    public int available() throws IOException {
      throw error;
    }
  }

  private static final class IOExceptionOnReadInputStream extends InputStream {

    private final IOException exception;

    private IOExceptionOnReadInputStream(IOException exception) {
      this.exception = exception;
    }

    @Override
    public int read() throws IOException {
      throw exception;
    }
  }

  private static final class ErrorOnReadInputStream extends InputStream {

    private final Error error;

    private ErrorOnReadInputStream(Error error) {
      this.error = error;
    }

    @Override
    public int read() {
      throw error;
    }
  }

  private static final class IOExceptionOnAvailableInputStream extends InputStream {

    private final IOException exception;

    private IOExceptionOnAvailableInputStream(IOException exception) {
      this.exception = exception;
    }

    @Override
    public int read() {
      return -1;
    }

    @Override
    public int available() throws IOException {
      throw exception;
    }
  }

  private static final class ErrorBuilder<T> extends Builder<T> {

    private final Error error;

    private ErrorBuilder(Error error) {
      this.error = error;
    }

    @Override
    public T build(Object data) {
      throw error;
    }
  }

  private static final class ProtocolReadConnectionExceptionConnection extends Connection {

    private final JedisConnectionException exception;

    private ProtocolReadConnectionExceptionConnection(JedisConnectionException exception) {
      this.exception = exception;
    }

    @Override
    protected Object protocolRead(RedisInputStream is, PushConsumerChain consumer) {
      throw exception;
    }
  }

  private static final class ProtocolReadDataExceptionConnection extends Connection {

    private final JedisDataException exception;

    private ProtocolReadDataExceptionConnection(JedisDataException exception) {
      this.exception = exception;
    }

    @Override
    protected Object protocolRead(RedisInputStream is, PushConsumerChain consumer) {
      throw exception;
    }
  }

  private static final class SuppliedConnectionFactory extends BasePooledObjectFactory<Connection> {

    private final Supplier<Connection> connectionSupplier;
    private final AtomicInteger destroyed;

    private SuppliedConnectionFactory(Supplier<Connection> connectionSupplier,
        AtomicInteger destroyed) {
      this.connectionSupplier = connectionSupplier;
      this.destroyed = destroyed;
    }

    @Override
    public Connection create() {
      return connectionSupplier.get();
    }

    @Override
    public PooledObject<Connection> wrap(Connection connection) {
      return new DefaultPooledObject<>(connection);
    }

    @Override
    public void destroyObject(PooledObject<Connection> pooledObject) throws Exception {
      destroyed.incrementAndGet();
      super.destroyObject(pooledObject);
    }
  }

  private static final class ErrorOnGetBuilderCommandObject extends CommandObject<Object> {

    private final Error error;

    private ErrorOnGetBuilderCommandObject(CommandArguments args, Error error) {
      super(args, BuilderFactory.RAW_OBJECT);
      this.error = error;
    }

    @Override
    public Builder<Object> getBuilder() {
      throw error;
    }
  }

  private static final class SyntheticError extends Error {
    private static final long serialVersionUID = 1L;
  }
}

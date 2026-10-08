package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.MaintenanceNotificationsConfig.Mode;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Unit tests for the controller-facing side of {@link MaintenanceAwareVisitor}: when the pool's
 * {@link MaintenanceController} is asked to register a connection and when that registration is
 * undone again — independent of which family the controller serves. The RESP3 handshake exchange
 * itself needs a server and is covered by the TcpMockServer-backed tests.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class MaintenanceAwareVisitorTest {

  @Mock
  private MaintenanceController controller;
  @Mock
  private Connection connection;

  @Test
  public void constructorRejectsNulls() {
    Connection.Builder builder = Connection.builder();
    assertThrows(IllegalArgumentException.class,
      () -> new MaintenanceAwareVisitor(null, controller));
    assertThrows(IllegalArgumentException.class, () -> new MaintenanceAwareVisitor(builder, null));
  }

  @Test
  public void beforeHandshakeRegistersTheConnectionWhenMaintenanceIsOn() {
    for (Mode mode : new Mode[] { Mode.AUTO, Mode.ENABLED }) {
      visitor(MaintenanceNotificationsConfig.builder().mode(mode).build())
          .visitBeforeHandshake(connection);
    }

    verify(controller, times(2)).register(connection);
    verify(controller, never()).unregister(any());
  }

  @Test
  public void beforeHandshakeSkipsTheControllerWhenMaintenanceIsOffOrUnset() {
    visitor(MaintenanceNotificationsConfig.DISABLED).visitBeforeHandshake(connection);
    visitor(null).visitBeforeHandshake(connection);

    verifyNoInteractions(controller, connection);
  }

  @Test
  public void afterHandshakeSkipsTheControllerWhenMaintenanceIsOffOrUnset() {
    visitor(MaintenanceNotificationsConfig.DISABLED).visitAfterHandshake(connection);
    visitor(null).visitAfterHandshake(connection);

    verifyNoInteractions(controller, connection);
  }

  @Test
  public void afterHandshakeOverResp2InAutoModeUnregistersQuietly() {
    when(connection.getRedisProtocol()).thenReturn(RedisProtocol.RESP2);

    visitor(MaintenanceNotificationsConfig.builder().mode(Mode.AUTO).build())
        .visitAfterHandshake(connection);

    // what visitBeforeHandshake installed is undone: this connection cannot carry the feature
    verify(controller).unregister(connection);
    verify(connection, never()).addPushConsumer(any());
  }

  @Test
  public void afterHandshakeOverResp2InEnabledModeUnregistersThenThrows() {
    when(connection.getRedisProtocol()).thenReturn(null); // a connection that never sent HELLO
    MaintenanceAwareVisitor visitor = visitor(
      MaintenanceNotificationsConfig.builder().mode(Mode.ENABLED).build());

    assertThrows(JedisConnectionException.class, () -> visitor.visitAfterHandshake(connection));

    verify(controller).unregister(connection);
  }

  private MaintenanceAwareVisitor visitor(MaintenanceNotificationsConfig maintConfig) {
    Connection.Builder builder = Connection.builder().maintenanceConfig(maintConfig);
    return new MaintenanceAwareVisitor(builder, controller);
  }
}

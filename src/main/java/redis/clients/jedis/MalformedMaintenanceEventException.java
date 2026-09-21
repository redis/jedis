package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisException;

/**
 * A resolved maintenance push frame whose fields are malformed. Signalled by
 * {@link MaintenancePushCodec#build};
 */
class MalformedMaintenanceEventException extends JedisException {

  MalformedMaintenanceEventException(String message) {
    super(message);
  }

  MalformedMaintenanceEventException(String message, Throwable cause) {
    super(message, cause);
  }
}

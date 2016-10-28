package redis.clients.jedis;

import java.util.Objects;

/**
 * Encapsulates redis (lua) routine as pair of
 * (sha1Digest digest; routine body plaintext; routine 'external' name, e.g. filename).
 * Comes handy if redis answers NOSCRIPT when asked for 'evalsha sha1Digest';
 * in this case user should retry with 'eval script_body'.
 */
public class RedisScriptingRoutine {
  private final String sha1Digest;
  private final String routineBody;
  private final String name;

  RedisScriptingRoutine(String sha1Digest, String name, String routineBody) {
    this.sha1Digest = sha1Digest;
    this.routineBody = routineBody;
    this.name = name;
  }

  public String getDigest() {
    return sha1Digest;
  }

  public String getRoutineBody() {
    return routineBody;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RedisScriptingRoutine routine = (RedisScriptingRoutine) o;
    return Objects.equals(sha1Digest, routine.sha1Digest) &&
            Objects.equals(routineBody, routine.routineBody) &&
            Objects.equals(name, routine.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sha1Digest, routineBody, name);
  }

  @Override
  public String toString() {
    return "RedisScriptingRoutine{sha1Digest='" + sha1Digest + "\', name='" + name + "\'}";
  }
}

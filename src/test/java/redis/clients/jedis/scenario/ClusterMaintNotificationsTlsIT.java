package redis.clients.jedis.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.redis.test.fi.Scenario;
import com.redis.test.fi.SlotMigrateEffect;

import org.junit.jupiter.api.Tag;

/**
 * S6: the end-to-end migration check of {@link ClusterMaintNotificationsIT} over {@code rediss://},
 * with the truststore pinned to the chain the database presents and default hostname verification.
 * The one row is remove-add / migrate on the hostname variant: Case 2 is where TLS can differ from
 * plaintext (a new pool handshakes to a node the client never connected to, under the open gate),
 * and the FQDN in SMIGRATED is what the certificate is verified against. Every S1 to S3 and S10
 * observation of the parent applies unchanged.
 * <p>
 * JUnit resolves {@code @MethodSource} against the executing test class, so the factory method here
 * replaces the parent's rows while the inherited {@code migrationIsHandled} runs as is.
 */
@Tag("scenario")
public class ClusterMaintNotificationsTlsIT extends ClusterMaintNotificationsIT {

  static Stream<Scenario> effectTriggerCombinations() {
    List<Scenario> rows = new ArrayList<>();
    row(rows, SlotMigrateEffect.REMOVE_ADD, "migrate", "external/hostname+tls");
    return rows.stream();
  }
}

package redis.clients.jedis;

public class ClientCapaConfig {

  private final boolean redirect;

  public static final ClientCapaConfig DEFAULT = new ClientCapaConfig(false);
  public static final ClientCapaConfig DISABLED = new ClientCapaConfig(false);

  public ClientCapaConfig() {
    this(false);
  }

  public ClientCapaConfig(boolean redirect) {
    this.redirect = redirect;
  }

  public boolean isRedirect() {
    return redirect;
  }

  public static ClientCapaConfig withRedirect() {
    return new ClientCapaConfig(true);
  }
}

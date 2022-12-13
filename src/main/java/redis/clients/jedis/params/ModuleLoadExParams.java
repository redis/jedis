package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.ARGS;
import static redis.clients.jedis.Protocol.Keyword.CONFIG;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.util.KeyValue;

public class ModuleLoadExParams implements IParams {

  private final List<KeyValue<String, String>> configs = new ArrayList<>();
  private final List<String> args = new ArrayList<>();

  public ModuleLoadExParams() {
  }

  public ModuleLoadExParams moduleLoadexParams() {
    return new ModuleLoadExParams();
  }

  public ModuleLoadExParams config(String name, String value) {
    this.configs.add(KeyValue.of(name, value));
    return this;
  }

  public ModuleLoadExParams arg(String arg) {
    this.args.add(arg);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    this.configs.forEach(kv -> args.add(CONFIG).add(kv.getKey()).add(kv.getValue()));

    if (!this.args.isEmpty()) {
      args.add(ARGS).addObjects(this.args);
    }
  }
}

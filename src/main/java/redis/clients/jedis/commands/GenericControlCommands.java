package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.Module;
import redis.clients.jedis.params.FailoverParams;

public interface GenericControlCommands extends ConfigCommands, ScriptingControlCommands, SlowlogCommands {

  String failover();

  String failover(FailoverParams failoverParams);

  String failoverAbort();

  List<Module> moduleList();
}

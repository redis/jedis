package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.args.Rawable;

public class CommandObject<T> {

  private final CommandArguments arguments;
  private final Builder<T> builder;

  /**
   * Hooks run against the {@link Connection} right before this command is sent, in list order, for
   * per-connection setup that must precede it &mdash; HIMPORT injects a lazy {@code PREPARE} ahead
   * of its {@code SET}, cluster ASK redirection appends an {@code ASKING} hook. Empty for ordinary
   * commands. The command still travels the normal {@code CommandExecutor} path;
   * {@link Connection#executeCommand(CommandObject)} invokes the hooks on the chosen connection,
   * then sends the command as usual. Hooks are excluded from {@link #equals}/{@link #hashCode}: a
   * hook-bearing copy is the same command.
   */
  private final List<Consumer<Connection>> preProcessHooks;

  public CommandObject(CommandArguments args, Builder<T> builder) {
    this(args, builder, Collections.emptyList());
  }

  CommandObject(CommandArguments args, Builder<T> builder,
      List<Consumer<Connection>> preProcessHooks) {
    this.arguments = args;
    this.builder = builder;
    // Defensive copy so no caller-retained reference can mutate this command; empty stays
    // allocation-free for ordinary commands.
    this.preProcessHooks = preProcessHooks.isEmpty() ? Collections.emptyList()
        : Collections.unmodifiableList(new ArrayList<>(preProcessHooks));
  }

  public CommandArguments getArguments() {
    return arguments;
  }

  public Builder<T> getBuilder() {
    return builder;
  }

  List<Consumer<Connection>> getPreProcessHooks() {
    return preProcessHooks;
  }

  /**
   * Returns a new command identical to this one with {@code hook} appended to its pre-process
   * hooks; this instance is immutable and unaffected. Hooks run in append order on the connection
   * right before the command is sent &mdash;
   */
  @Experimental
  public CommandObject<T> withPreProcessHook(Consumer<Connection> hook) {
    List<Consumer<Connection>> hooks = new ArrayList<>(preProcessHooks.size() + 1);
    hooks.addAll(preProcessHooks);
    hooks.add(hook);
    return new CommandObject<>(arguments, builder, hooks);
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    for (Rawable e : arguments) {
      hashCode = 31 * hashCode + e.hashCode();
    }
    hashCode = 31 * hashCode + builder.hashCode();
    return hashCode;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof CommandObject)) {
      return false;
    }

    Iterator<Rawable> e1 = arguments.iterator();
    Iterator<Rawable> e2 = ((CommandObject) o).arguments.iterator();
    while (e1.hasNext() && e2.hasNext()) {
      Rawable o1 = e1.next();
      Rawable o2 = e2.next();
      if (!(o1 == null ? o2 == null : o1.equals(o2))) {
        return false;
      }
    }
    if (e1.hasNext() || e2.hasNext()) {
      return false;
    }

    return builder == ((CommandObject) o).builder;
  }
}

package redis.clients.jedis.resps;

import redis.clients.jedis.Builder;

import static redis.clients.jedis.BuilderFactory.STRING;

import java.util.ArrayList;
import java.util.List;

public class CommandDocument {
  private final String summary;
  private final String since;
  private final String group;
  private final String complexity;
  private final List<String> history;

  public CommandDocument(String summary, String since, String group, String complexity, List<String> history) {
    this.summary = summary;
    this.since = since;
    this.group = group;
    this.complexity = complexity;
    this.history = history;
  }

  public String getSummary() {
    return summary;
  }

  public String getSince() {
    return since;
  }

  public String getGroup() {
    return group;
  }

  public String getComplexity() {
    return complexity;
  }

  public List<String> getHistory() {
    return history;
  }

  public static final Builder<CommandDocument> COMMAND_DOCUMENT_BUILDER = new Builder<CommandDocument>() {
    @Override
    public CommandDocument build(Object data) {
      List<Object> commandData = (List<Object>) data;
      String summary = STRING.build(commandData.get(1));
      String since = STRING.build(commandData.get(3));
      String group = STRING.build(commandData.get(5));
      String complexity = STRING.build(commandData.get(7));
      List<String> history = null;
      if (STRING.build(commandData.get(8)).equals("history")) {
        List<List<Object>> rawHistory = (List<List<Object>>) commandData.get(9);
        history = new ArrayList<>(rawHistory.size());
        for (List<Object> timePoint : rawHistory) {
          history.add(STRING.build(timePoint.get(0)) + ": " + STRING.build(timePoint.get(1)));
        }
      }
      return new CommandDocument(summary, since, group, complexity, history);
    }
  };
}

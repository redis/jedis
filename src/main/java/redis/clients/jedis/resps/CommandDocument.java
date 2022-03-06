package redis.clients.jedis.resps;

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
}

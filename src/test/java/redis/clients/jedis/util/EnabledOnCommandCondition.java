package redis.clients.jedis.util;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import redis.clients.jedis.*;
import redis.clients.jedis.resps.CommandInfo;

import java.util.Map;
import java.util.Optional;

public class EnabledOnCommandCondition implements ExecutionCondition {

  private final HostAndPort hostPort;
  private final JedisClientConfig config;

  public EnabledOnCommandCondition(HostAndPort hostPort, JedisClientConfig config) {
    this.hostPort = hostPort;
    this.config = config;
  }

  public EnabledOnCommandCondition(EndpointConfig endpointConfig) {
    this.hostPort = endpointConfig.getHostAndPort();
    this.config = endpointConfig.getClientConfigBuilder().build();
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    try (Jedis jedisClient = new Jedis(hostPort, config)) {
      String[] command = getCommandFromAnnotations(context);

      if (command != null && !isCommandAvailable(jedisClient, command[0], command[1])) {
        return ConditionEvaluationResult.disabled(
            "Test requires Redis command '" + command[0] + " " + command[1]
                + "' to be available on " + hostPort + ", but it was not found.");
      }
    } catch (Exception e) {
      return ConditionEvaluationResult.disabled(
          "Failed to check Redis command on " + hostPort + ": " + e.getMessage());
    }
    return ConditionEvaluationResult.enabled("Redis command is available");
  }

  private String[] getCommandFromAnnotations(ExtensionContext context) {
    Optional<EnabledOnCommand> methodAnnotation = AnnotationUtils.findAnnotation(
        context.getRequiredTestMethod(), EnabledOnCommand.class);
    if (methodAnnotation.isPresent()) {
      return new String[] { methodAnnotation.get().value(), methodAnnotation.get().subCommand() };
    }

    Optional<EnabledOnCommand> classAnnotation = AnnotationUtils.findAnnotation(
        context.getRequiredTestClass(), EnabledOnCommand.class);
    if (classAnnotation.isPresent()) {
      return new String[] { classAnnotation.get().value(), classAnnotation.get().subCommand() };
    }

    return null;
  }

  private boolean isCommandAvailable(Jedis jedisClient, String command, String subCommand) {
    try {
      Map<String, CommandInfo> commandInfoMap = jedisClient.commandInfo(command);
      CommandInfo commandInfo = commandInfoMap.get(command.toLowerCase());
      if (commandInfo != null) {
        if (subCommand != null && !subCommand.isEmpty()) {
          String replySubCommandName = command + '|' + subCommand;
          for (CommandInfo supportedSubCommand : commandInfo.getSubcommands().values()) {
            if (replySubCommandName.equalsIgnoreCase(supportedSubCommand.getName())) {
              return true;
            }
          }
          return false;
        }
        return true;
      }
      return false;
    } catch (Exception e) {
      throw new RuntimeException("Error found while EnableOnCommand for command '" + command + "'",
          e);
    }
  }

}
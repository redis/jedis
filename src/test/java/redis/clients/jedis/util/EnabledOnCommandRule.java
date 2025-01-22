package redis.clients.jedis.util;

import io.redis.test.annotations.EnabledOnCommand;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import redis.clients.jedis.*;
import redis.clients.jedis.resps.CommandInfo;

public class EnabledOnCommandRule implements TestRule {

    private final HostAndPort hostPort;
    private final JedisClientConfig config;

    public EnabledOnCommandRule(HostAndPort hostPort, JedisClientConfig config) {
        this.hostPort = hostPort;
        this.config = config;
    }

    public EnabledOnCommandRule(EndpointConfig endpointConfig) {
        this.hostPort = endpointConfig.getHostAndPort();
        this.config = endpointConfig.getClientConfigBuilder().build();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try (Jedis jedisClient = new Jedis(hostPort, config)) {
                    String[] command = getCommandFromAnnotations(description);

                    if (command != null && !isCommandAvailable(jedisClient, command[0],command[1])) {
                        Assume.assumeTrue("Test requires Redis command '" + command[0] + " " + command[1] + "' to be available, but it was not found.", false);
                    }

                    base.evaluate();
                }
            }

            /**
             * Retrieves the command from either class-level or method-level annotations.
             *
             * @param description The test description containing annotations.
             * @return The Redis array containing command, subcommand from the annotations, or null if not found.
             */
            private String[] getCommandFromAnnotations(Description description) {
                // Retrieve class-level and method-level annotations
                EnabledOnCommand descriptionCommandAnnotation = description.getAnnotation(EnabledOnCommand.class);
                if (descriptionCommandAnnotation != null) {
                    return new String[] {descriptionCommandAnnotation.value(), descriptionCommandAnnotation.subCommand()};
                }

                EnabledOnCommand methodCommand = getMethodAnnotation(description);
                if (methodCommand != null) {
                    return new String[] {methodCommand.value(), methodCommand.subCommand()};
                }

                EnabledOnCommand classCommand = description.getTestClass().getAnnotation(EnabledOnCommand.class);
                if (classCommand != null) {
                    return new String[] {classCommand.value(), classCommand.subCommand()};
                }

                return null;
            }

            private EnabledOnCommand getMethodAnnotation(Description description) {
                try {
                    // description.getAnnotation() does not return anootaion when used
                    // with parametrised tests
                    String methodName = description.getMethodName();
                    if (methodName != null) {
                        Class<?> testClass = description.getTestClass();
                        if (testClass != null) {
                            for (Method method : testClass.getDeclaredMethods()) {
                                if (method.getName().equals(methodName)) {
                                    return method.getAnnotation(EnabledOnCommand.class);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Handle any potential exceptions here
                    throw  new RuntimeException("Could not resolve EnabledOnCommand annotation",e);
                }
                return null;
            }

            /**
             * Checks if the specified Redis command is available.
             */
            private boolean isCommandAvailable(Jedis jedisClient, String command, String subCommand) {
                try {
                    Map<String, CommandInfo> commandInfoMap= jedisClient.commandInfo(command);
                    CommandInfo commandInfo = commandInfoMap.get(command.toLowerCase());
                    if (commandInfo != null) {
                        // If a subCommand is provided, check for the subcommand under this command
                        if (subCommand != null && !subCommand.isEmpty()) {
                            // Check if this command supports the provided subcommand
                            String replySubCommandName = command + '|' + subCommand;
                            for (CommandInfo supportedSubCommand : commandInfo.getSubcommands().values()) {
                                if (replySubCommandName.equalsIgnoreCase(supportedSubCommand.getName())) {
                                    return true;
                                }
                            }
                            return false; // Subcommand not found
                        }
                        return true; // Command found (no subcommand required)
                    }
                    return false; // Command not found
                } catch (Exception e) {
                    String msg = String.format("Error found while EnableOnCommand for command '%s'", command);
                   throw new RuntimeException(msg, e);
                }
            }
        };
    }
}

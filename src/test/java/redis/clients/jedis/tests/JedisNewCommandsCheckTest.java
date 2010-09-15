package redis.clients.jedis.tests;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Transaction;

public class JedisNewCommandsCheckTest extends Assert {
    @Test
    @Ignore(value = "Ignored because still missing information for DEBUG and LINSERT commands")
    public void checkJedisIsUpdated() throws IOException {
	String[] commands = getAvailableCommands();
	Set<String> implementedCommands = getImplementedCommands();

	Set<String> missingCommands = new HashSet<String>();
	for (String command : commands) {
	    if (!implementedCommands.contains(command.trim())) {
		missingCommands.add(command);
	    }
	}

	if (!missingCommands.isEmpty()) {
	    fail("There are missing commands: " + missingCommands.toString());
	}
    }

    private Set<String> getImplementedCommands() {
	Method[] methods = Jedis.class.getDeclaredMethods();
	Set<String> implementedCommands = new HashSet<String>();
	for (Method method : methods) {
	    implementedCommands.add(method.getName().trim().toLowerCase());
	}

	methods = JedisPubSub.class.getDeclaredMethods();
	for (Method method : methods) {
	    implementedCommands.add(method.getName().trim().toLowerCase());
	}

	methods = Transaction.class.getDeclaredMethods();
	for (Method method : methods) {
	    implementedCommands.add(method.getName().trim().toLowerCase());
	}
	implementedCommands.add("config");
	return implementedCommands;
    }

    private String[] getAvailableCommands() throws MalformedURLException,
	    IOException {
	URL url = new URL("http://dimaion.com/redis/master");
	InputStream openStream = url.openStream();
	DataInputStream dis = new DataInputStream(new BufferedInputStream(
		openStream));
	byte[] all = new byte[dis.available()];
	dis.readFully(all);
	String commandList = new String(all);
	String[] commands = commandList.split("\n");
	return commands;
    }
}

package redis.clients.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

public class JedisByteHashMapTest {
	@Test
	public void canSerializeEmptyJedisByteHashMap() throws Exception {
		JedisByteHashMap map = new JedisByteHashMap();
		serializeAndDeserialize(map);
	}

	private void serializeAndDeserialize(JedisByteHashMap map) throws IOException,
			ClassNotFoundException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		objectOutputStream.writeObject(map);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
		objectInputStream.readObject();
	}
	
}

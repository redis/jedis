package redis.clients.jedis.unix;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import redis.clients.jedis.Jedis;

public class UnixDomainJedis extends Jedis {
    
    public UnixDomainJedis(final String socketPath) throws IOException{
    	super(getSocket(socketPath));
    }
    
    private static Socket getSocket(String url) throws IOException{
    	File socketFile = new File(url);
    	AFUNIXSocket socket = AFUNIXSocket.newInstance();
    	socket.connect(new AFUNIXSocketAddress(socketFile));
    	return socket;
    }
}

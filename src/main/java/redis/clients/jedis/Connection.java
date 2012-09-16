package redis.clients.jedis;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

public class Connection {
    private String host;
    private int port = Protocol.DEFAULT_PORT;
    private Socket socket;
    private RedisOutputStream outputStream;
    private RedisInputStream inputStream;
    private int pipelinedCommands = 0;
    private int timeout = Protocol.DEFAULT_TIMEOUT;

    private Thread pumpThread;
    BlockingQueue<Response<?>> asyncResponses = new LinkedBlockingQueue<Response<?>>();


    public Socket getSocket() {
        return socket;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public void setTimeoutInfinite() {
        try {
            if(!isConnected()) {
        	connect();
            }
            socket.setKeepAlive(true);
            socket.setSoTimeout(0);
        } catch (SocketException ex) {
            throw new JedisException(ex);
        }
    }

    public void rollbackTimeout() {
        try {
            socket.setSoTimeout(timeout);
            socket.setKeepAlive(false);
        } catch (SocketException ex) {
            throw new JedisException(ex);
        }
    }

    public Connection(final String host) {
        super();
        this.host = host;
    }

    protected void flush() {
        try {
            outputStream.flush();
        } catch (IOException e) {
            throw new JedisConnectionException(e);
        }
    }

    protected Connection sendCommand(final Command cmd, final String... args) {
        final byte[][] bargs = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            bargs[i] = SafeEncoder.encode(args[i]);
        }
        return sendCommand(cmd, bargs);
    }

    protected Connection sendCommand(final Command cmd, final byte[]... args) {
        connect();
        Protocol.sendCommand(outputStream, cmd, args);
        pipelinedCommands++;
        return this;
    }
    
    protected Connection sendCommand(final Command cmd) {
        connect();
        Protocol.sendCommand(outputStream, cmd, new byte[0][]);
        pipelinedCommands++;
        return this;
    }

    public Connection(final String host, final int port) {
        super();
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public Connection() {
    	
    }

    public void connect() {
        if (!isConnected()) {
            try {
                socket = new Socket();
                //->@wjw_add
                socket.setReuseAddress(true);
                socket.setKeepAlive(true);  //Will monitor the TCP connection is valid
                socket.setTcpNoDelay(true);  //Socket buffer Whetherclosed, to ensure timely delivery of data
                socket.setSoLinger(true,0);  //Control calls close () method, the underlying socket is closed immediately
                //<-@wjw_add

                socket.connect(new InetSocketAddress(host, port), timeout);
                socket.setSoTimeout(timeout);
                outputStream = new RedisOutputStream(socket.getOutputStream());
                inputStream = new RedisInputStream(socket.getInputStream());
            } catch (IOException ex) {
                throw new JedisConnectionException(ex);
            }
        }
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                inputStream.close();
                outputStream.close();
                if (!socket.isClosed()) {
                    socket.close();
                }
                if(pumpThread != null){
                    pumpThread.interrupt();
                    pumpThread.join();
                }
            } catch (IOException ex) {
                throw new JedisConnectionException(ex);
            } catch (InterruptedException ie){
                // throw?
            }
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isBound() && !socket.isClosed()
                && socket.isConnected() && !socket.isInputShutdown()
                && !socket.isOutputShutdown();
    }

    protected String getStatusCodeReply() {
        final byte[] resp = (byte[]) getOne();
        if (null == resp) {
            return null;
        } else {
            return SafeEncoder.encode(resp);
        }
    }

    public String getBulkReply() {
        final byte[] result = getBinaryBulkReply();
        if (null != result) {
            return SafeEncoder.encode(result);
        } else {
            return null;
        }
    }

    public byte[] getBinaryBulkReply() {
        return (byte[]) getOne();
    }

    public Long getIntegerReply() {
        return (Long) getOne();
    }

    public List<String> getMultiBulkReply() {
        return BuilderFactory.STRING_LIST.build(getBinaryMultiBulkReply());
    }

    @SuppressWarnings("unchecked")
    public List<byte[]> getBinaryMultiBulkReply() {
        return (List<byte[]>) getOne();
    }

    @SuppressWarnings("unchecked")
    public List<Object> getObjectMultiBulkReply() {
        return (List<Object>) getOne();
    }
    
    @SuppressWarnings("unchecked")
    public List<Long> getIntegerMultiBulkReply() {
        return (List<Long>) getOne();
    }

    public List<Object> getAll() {
        return getAll(0);
    }

    public List<Object> getAll(int except) {
        List<Object> all = new ArrayList<Object>();
        flush();
        while (pipelinedCommands > except) {
        	try{
                all.add(Protocol.read(inputStream));
        	}catch(JedisDataException e){
        		all.add(e);
        	}
            pipelinedCommands--;
        }
        return all;
    }

    public Object getOne() {
        waitForAsyncReadsToComplete();
        flush();
        pipelinedCommands--;
        return Protocol.read(inputStream);
    }

    public void flushAsync(Collection<Response<?>> responses) {
        flush();
        asyncResponses.addAll(responses);
        startResponsePump();
    }

    private void startResponsePump() {
        if (pumpThread == null) {
            pumpThread = new Thread(new ResponsePump());
            pumpThread.start();
        }
    }

    private void wakeNextWriter() {
        if (asyncResponses.isEmpty()) {
            synchronized (pumpThread) {
                pumpThread.notify();
            }
        }
    }

    private void waitForAsyncReadsToComplete() {
        if (!asyncResponses.isEmpty()) {
            while (!asyncResponses.isEmpty()) {
                try {
                    synchronized (pumpThread) {
                        pumpThread.wait();
                    }
                } catch (InterruptedException e) {
                    // wait some more
                }
            }
        }
    }

    private class ResponsePump implements Runnable {
        @Override
        public void run() {
            JedisDataException exception = null;
            try {
                while (isConnected()) {
                    Response<?> response = asyncResponses.take();
                    Object data = Protocol.read(inputStream);
                    response.set(data);
                    wakeNextWriter();
                }

            } catch (Exception ioe) {
                exception = new JedisDataException(ioe);
            }
            if (exception == null) {
                exception = new JedisDataException("Disconnected");
            }
            // if something went wrong
            Response<?> response;
            while ((response = asyncResponses.poll()) != null) {
                response.set(exception);
            }
        }
    }
}

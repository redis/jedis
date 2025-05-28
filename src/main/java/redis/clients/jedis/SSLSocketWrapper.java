package redis.clients.jedis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.function.BiFunction;
import java.util.List;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

public class SSLSocketWrapper extends SSLSocket {

  SSLSocket actual;
  Socket underlying;
  InputStream wrapper;

  private class InputStreamWrapper extends InputStream {
    private InputStream actual;
    private InputStream underlying;

    public InputStreamWrapper(InputStream actual, InputStream underlying) {
      this.actual = actual;
      this.underlying = underlying;
    }

    @Override
    public int read() throws IOException {
      return actual.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
      return actual.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return actual.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
      return actual.skip(n);
    }

    @Override
    public int available() throws IOException {
      return underlying.available();
    }

    @Override
    public void close() throws IOException {
      actual.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
      actual.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
      actual.reset();
    }

    @Override
    public boolean markSupported() {
      return actual.markSupported();
    }
  }

  public SSLSocketWrapper(SSLSocket actual, Socket underlying) throws IOException {
    this.actual = actual;
    this.underlying = underlying;
    this.wrapper = new InputStreamWrapper(actual.getInputStream(), underlying.getInputStream());
  }

  @Override
  public void connect(SocketAddress endpoint) throws IOException {
    actual.connect(endpoint);
  }

  @Override
  public void connect(SocketAddress endpoint, int timeout) throws IOException {
    actual.connect(endpoint, timeout);
  }

  @Override
  public void bind(SocketAddress bindpoint) throws IOException {
    actual.bind(bindpoint);
  }

  @Override
  public InetAddress getInetAddress() {
    return actual.getInetAddress();
  }

  @Override
  public InetAddress getLocalAddress() {
    return actual.getLocalAddress();
  }

  @Override
  public int getPort() {
    return actual.getPort();
  }

  @Override
  public int getLocalPort() {
    return actual.getLocalPort();
  }

  @Override
  public SocketAddress getRemoteSocketAddress() {
    return actual.getRemoteSocketAddress();
  }

  @Override
  public SocketAddress getLocalSocketAddress() {
    return actual.getLocalSocketAddress();
  }

  @Override
  public void setTcpNoDelay(boolean on) throws SocketException {
    actual.setTcpNoDelay(on);
  }

  @Override
  public boolean getTcpNoDelay() throws SocketException {
    return actual.getTcpNoDelay();
  }

  @Override
  public void setSoLinger(boolean on, int linger) throws SocketException {
    actual.setSoLinger(on, linger);
  }

  @Override
  public int getSoLinger() throws SocketException {
    return actual.getSoLinger();
  }

  @Override
  public void sendUrgentData(int data) throws IOException {
    actual.sendUrgentData(data);
  }

  @Override
  public void setOOBInline(boolean on) throws SocketException {
    actual.setOOBInline(on);
  }

  @Override
  public boolean getOOBInline() throws SocketException {
    return actual.getOOBInline();
  }

  @Override
  public synchronized void setSoTimeout(int timeout) throws SocketException {
    actual.setSoTimeout(timeout);
  }

  @Override
  public synchronized int getSoTimeout() throws SocketException {
    return actual.getSoTimeout();
  }

  @Override
  public synchronized void setSendBufferSize(int size) throws SocketException {
    actual.setSendBufferSize(size);
  }

  @Override
  public synchronized int getSendBufferSize() throws SocketException {
    return actual.getSendBufferSize();
  }

  @Override
  public synchronized void setReceiveBufferSize(int size) throws SocketException {
    actual.setReceiveBufferSize(size);
  }

  @Override
  public synchronized int getReceiveBufferSize() throws SocketException {
    return actual.getReceiveBufferSize();
  }

  @Override
  public void setKeepAlive(boolean on) throws SocketException {
    actual.setKeepAlive(on);
  }

  @Override
  public boolean getKeepAlive() throws SocketException {
    return actual.getKeepAlive();
  }

  @Override
  public void setTrafficClass(int tc) throws SocketException {
    actual.setTrafficClass(tc);
  }

  @Override
  public int getTrafficClass() throws SocketException {
    return actual.getTrafficClass();
  }

  @Override
  public void setReuseAddress(boolean on) throws SocketException {
    actual.setReuseAddress(on);
  }

  @Override
  public boolean getReuseAddress() throws SocketException {
    return actual.getReuseAddress();
  }

  @Override
  public synchronized void close() throws IOException {
    actual.close();
  }

  @Override
  public void shutdownInput() throws IOException {
    actual.shutdownInput();
  }

  @Override
  public void shutdownOutput() throws IOException {
    actual.shutdownOutput();
  }

  @Override
  public String toString() {
    return actual.toString();
  }

  @Override
  public boolean isConnected() {
    return actual.isConnected();
  }

  @Override
  public boolean isBound() {
    return actual.isBound();
  }

  @Override
  public boolean isClosed() {
    return actual.isClosed();
  }

  @Override
  public boolean isInputShutdown() {
    return actual.isInputShutdown();
  }

  @Override
  public boolean isOutputShutdown() {
    return actual.isOutputShutdown();
  }

  @Override
  public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    actual.setPerformancePreferences(connectionTime, latency, bandwidth);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return wrapper;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return actual.getOutputStream();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return actual.getSupportedCipherSuites();
  }

  @Override
  public String[] getEnabledCipherSuites() {
    return actual.getEnabledCipherSuites();
  }

  @Override
  public void setEnabledCipherSuites(String[] var1) {
    actual.setEnabledCipherSuites(var1);
  }

  @Override
  public String[] getSupportedProtocols() {
    return actual.getSupportedProtocols();
  }

  @Override
  public String[] getEnabledProtocols() {
    return actual.getEnabledProtocols();
  }

  @Override
  public void setEnabledProtocols(String[] var1) {
    actual.setEnabledProtocols(var1);
  }

  @Override
  public SSLSession getSession() {
    return actual.getSession();
  }

  @Override
  public SSLSession getHandshakeSession() {
    return actual.getHandshakeSession();
  }

  @Override
  public void addHandshakeCompletedListener(HandshakeCompletedListener var1) {
    actual.addHandshakeCompletedListener(var1);
  }

  @Override
  public void removeHandshakeCompletedListener(HandshakeCompletedListener var1) {
    actual.removeHandshakeCompletedListener(var1);
  }

  @Override
  public void startHandshake() throws IOException {
    actual.startHandshake();
  }

  @Override
  public void setUseClientMode(boolean var1) {
    actual.setUseClientMode(var1);
  }

  @Override
  public boolean getUseClientMode() {
    return actual.getUseClientMode();
  }

  @Override
  public void setNeedClientAuth(boolean var1) {
    actual.setNeedClientAuth(var1);
  }

  @Override
  public boolean getNeedClientAuth() {
    return actual.getNeedClientAuth();
  }

  @Override
  public void setWantClientAuth(boolean var1) {
    actual.setWantClientAuth(var1);
  }

  @Override
  public boolean getWantClientAuth() {
    return actual.getWantClientAuth();
  }

  @Override
  public void setEnableSessionCreation(boolean var1) {
    actual.setEnableSessionCreation(var1);
  }

  @Override
  public boolean getEnableSessionCreation() {
    return actual.getEnableSessionCreation();
  }

  @Override
  public SSLParameters getSSLParameters() {
    return actual.getSSLParameters();
  }

  @Override
  public void setSSLParameters(SSLParameters var1) {
    actual.setSSLParameters(var1);
  }

  @Override
  public String getApplicationProtocol() {
    return actual.getApplicationProtocol();
  }

  @Override
  public String getHandshakeApplicationProtocol() {
    return actual.getHandshakeApplicationProtocol();
  }

  @Override
  public void setHandshakeApplicationProtocolSelector(BiFunction<SSLSocket, List<String>, String> var1) {
    actual.setHandshakeApplicationProtocolSelector(var1);
  }

  @Override
  public BiFunction<SSLSocket, List<String>, String> getHandshakeApplicationProtocolSelector() {
    return actual.getHandshakeApplicationProtocolSelector();
  }
}

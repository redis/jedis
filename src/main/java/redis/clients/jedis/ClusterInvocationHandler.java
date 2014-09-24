package redis.clients.jedis;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterMaxRedirectionsException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.exceptions.JedisRedirectionException;
import redis.clients.util.JedisClusterCRC16;

/**
 * Alternative Redis Cluster command object supporting binary commands and
 * string commands. Uses the builtin Java Proxy API to create a very compact
 * implementation. The main code path is optimized to reduce branches for the
 * common case of already having an accurate map of slots to nodes.
 *
 *
 */
public class ClusterInvocationHandler implements InvocationHandler {

    private final JedisClusterConnectionHandler connectionHandler;

    public interface BinaryAndStringCommands extends JedisCommands, BinaryJedisCommands {
    }

    public static BinaryAndStringCommands getProxy(Set<HostAndPort> nodes) {
        return getProxy(nodes, new GenericObjectPoolConfig());
    }

    public static BinaryAndStringCommands getProxy(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig) {
        return (BinaryAndStringCommands) Proxy.newProxyInstance(ClusterInvocationHandler.class.getClassLoader(),
                new Class<?>[]{BinaryAndStringCommands.class},
                new ClusterInvocationHandler(nodes, poolConfig));
    }

    public ClusterInvocationHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig) {
        this.connectionHandler = new JedisSlotBasedConnectionHandler(nodes, poolConfig);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        byte[] keyBytes = null;

        if (args[0].getClass().equals(String.class)) {
            keyBytes = args[0].toString().getBytes(Protocol.CHARSET);
        } else {
            keyBytes = (byte[]) args[0];
        }

        Jedis conn = this.connectionHandler.getConnectionFromSlot(JedisClusterCRC16.getSlot(keyBytes));

        try {
            for (int numRedirects = 0; numRedirects < 5; ++numRedirects) {

                try {
                    return method.invoke(conn, args);
                } catch (InvocationTargetException ite) {
                    Throwable cause = ite.getCause();

                    if (cause instanceof JedisConnectionException) {
                        this.connectionHandler.returnBrokenConnection(conn);

                        conn = this.connectionHandler.getConnection();
                    } else if (cause instanceof JedisRedirectionException) {
                        JedisRedirectionException jre = (JedisRedirectionException) ite.getCause();

                        if (jre instanceof JedisAskDataException) {
                            this.connectionHandler.assignSlotToNode(jre.getSlot(),
                                    jre.getTargetNode());

                            this.connectionHandler.returnConnection(conn);

                            conn = this.connectionHandler.getConnectionFromSlot(jre.getSlot());

                            conn.asking();
                        } else if (jre instanceof JedisMovedDataException) {
                            JedisMovedDataException mde = (JedisMovedDataException) jre;

                            this.connectionHandler.returnConnection(conn);

                            conn = this.connectionHandler.getConnectionFromSlot(mde.getSlot());

                            // it rebuilds cluster's slot cache
                            // recommended by Redis cluster specification
                            this.connectionHandler.renewSlotCache();
                        } else {
                            throw jre;
                        }
                    }//else
                    else {
                        throw ite;
                    }
                }
            }//for
        }//try
        finally {
            if (null != conn) {
                this.connectionHandler.returnConnection(conn);
                conn = null;
            }
        }

        throw new JedisClusterMaxRedirectionsException(
                "Too many Cluster redirections?");
    }

    public static Map<String, JedisPool> getClusterNodes(BinaryAndStringCommands proxy) {
        ClusterInvocationHandler handler = (ClusterInvocationHandler) Proxy.getInvocationHandler(proxy);
        return handler.connectionHandler.getNodes();
    }

    public static void shutdown(BinaryAndStringCommands proxy) {
        ClusterInvocationHandler handler = (ClusterInvocationHandler) Proxy.getInvocationHandler(proxy);
        for (JedisPool pool : handler.connectionHandler.getNodes().values()) {
            if (null != pool) {
                pool.destroy();
            }
        }
    }
}

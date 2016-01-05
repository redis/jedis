package redis.clients.util;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class BufferPool {
  private static final class BufferFactory extends BasePooledObjectFactory<byte[]> {
    static final BufferFactory INSTANCE = new BufferFactory();

    @Override
    public byte[] create() {
      return new byte[pooled_buffer_size];
    }

    @Override
    public PooledObject<byte[]> wrap(byte[] obj) {
      return new DefaultPooledObject<byte[]>(obj);
    }

    @Override
    public boolean validateObject(PooledObject<byte[]> p) {
      return p.getObject().length == pooled_buffer_size;
    }
  }

  private BufferPool() {
  }

  /**
   * @param newSize. Set to a negative value to disable buffer pooling
   */
  public static void setPooledBufferSize(final int newSize) {
    if (pooled_buffer_size != newSize) {
      buffer_pool.clear();
      pooled_buffer_size = newSize;
    }
  }

  public static int getPooledBufferSize() {
    return pooled_buffer_size;
  }

  /**
   * Application-wide setting
   */
  private static int pooled_buffer_size = IOUtils.DEFAULT_BUFFER_SIZE;
  private static GenericObjectPool<byte[]> buffer_pool = new GenericObjectPool<byte[]>(
      BufferFactory.INSTANCE);

  static {
    buffer_pool.setMinIdle(0);
    buffer_pool.setMaxWaitMillis(0L);
    buffer_pool.setTestOnBorrow(true); // pooled_buffer_size may have changed in between
    buffer_pool.setTestOnReturn(false);
    buffer_pool.setTestOnCreate(false);
    buffer_pool.setTestWhileIdle(false);
  }

  public static byte[] obtainBuffer(int size) {
    if (size == pooled_buffer_size) {
      try {
        return buffer_pool.borrowObject();
      } catch (Exception e) {
      }
    }
    return new byte[size];
  }

  public static void returnBuffer(byte[] buf) {
    if (buf.length == pooled_buffer_size) {
      try {
        buffer_pool.returnObject(buf);
      } catch (Exception e) {
      }
    }
  }
}

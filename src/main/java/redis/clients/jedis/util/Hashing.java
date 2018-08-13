package redis.clients.jedis.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public interface Hashing {
  Hashing MURMUR_HASH = new MurmurHash();
  ThreadLocal<MessageDigest> md5Holder = new ThreadLocal<MessageDigest>();

  Hashing MD5 = new Hashing() {
    @Override
    public long hash(String key) {
      return hash(SafeEncoder.encode(key));
    }

    @Override
    public long hash(byte[] key) {
      try {
        if (md5Holder.get() == null) {
          md5Holder.set(MessageDigest.getInstance("MD5"));
        }
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("++++ no md5 algorithm found");
      }
      MessageDigest md5 = md5Holder.get();

      md5.reset();
      md5.update(key);
      byte[] bKey = md5.digest();
      long res = ((long) (bKey[3] & 0xFF) << 24) | ((long) (bKey[2] & 0xFF) << 16)
          | ((long) (bKey[1] & 0xFF) << 8) | (long) (bKey[0] & 0xFF);
      return res;
    }
  };

  long hash(String key);

  long hash(byte[] key);
}
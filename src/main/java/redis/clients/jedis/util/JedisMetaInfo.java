package redis.clients.jedis.util;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jedis Meta info load version groupId
 */
public class JedisMetaInfo {
    private static final Logger log = LoggerFactory.getLogger(JedisMetaInfo.class);

    private static String groupId;
    private static String artifactId;
    private static String version;

    static {
        Properties p = new Properties();
        try {
            InputStream in = JedisMetaInfo.class.getClassLoader().getResourceAsStream("pom.properties");
            p.load(in);

            groupId = p.getProperty("groupId", null);
            artifactId = p.getProperty("artifactId", null);
            version = p.getProperty("version", null);

            in.close();
        } catch (Exception e) {
            log.error("Load Jedis meta info from pom.properties failed", e);
        }
    }

    public static String getGroupId() {
        return groupId;
    }

    public static String getArtifactId() {
        return artifactId;
    }

    public static String getVersion() {
        return version;
    }
}

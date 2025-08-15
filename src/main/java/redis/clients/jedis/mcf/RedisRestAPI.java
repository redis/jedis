package redis.clients.jedis.mcf;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParser;

import redis.clients.jedis.RedisCredentials;

/**
 * Helper class to check the availability of a Redis database
 */
class RedisRestAPI {

    private static final Logger log = LoggerFactory.getLogger(RedisRestAPI.class);
    private static final String BDBS_URL = "https://%s:%s/v1/bdbs?fields=uid";
    private static final String AVAILABILITY_URL = "https://%s:%s/v1/bdbs/%s/availability";
    private static final String LAGAWARE_AVAILABILITY_URL = "https://%s:%s/v1/bdbs/%s/availability?extend_check=lag";

    private Endpoint endpoint;
    private Supplier<RedisCredentials> credentialsSupplier;
    private int timeoutMs;
    private String bdbsUri;

    public RedisRestAPI(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier) {
        this(endpoint, credentialsSupplier, 1000);
    }

    public RedisRestAPI(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier, int timeoutMs) {
        this.endpoint = endpoint;
        this.credentialsSupplier = credentialsSupplier;
        this.timeoutMs = timeoutMs;
    }

    public List<String> getBdbs() throws IOException {
        if (bdbsUri == null) {
            bdbsUri = String.format(BDBS_URL, endpoint.getHost(), endpoint.getPort());
        }

        HttpURLConnection conn = null;
        try {
            conn = createConnection(bdbsUri, "GET", credentialsSupplier.get());
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            String responseBody = readResponse(conn);
            if (code != 200) {
                throw new IOException("Unexpected response code '" + code + "' for getBdbs: '" + responseBody
                    + "' from '" + bdbsUri + "'");
            }
            return parseUidsFromResponse(responseBody);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public boolean checkBdbAvailability(String uid, boolean lagAware) throws IOException {
        String availabilityUri = String.format(lagAware ? LAGAWARE_AVAILABILITY_URL : AVAILABILITY_URL,
            endpoint.getHost(), endpoint.getPort(), uid);
        HttpURLConnection conn = null;
        try {
            conn = createConnection(availabilityUri, "GET", credentialsSupplier.get());
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code == 200) {
                return true;
            }
            String body = readResponse(conn);
            log.warn("Availability check for {} returned body='{}' from '{}'", uid, body, availabilityUri);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    HttpURLConnection createConnection(String urlString, String method, RedisCredentials credentials)
        throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setRequestProperty("Authorization", getAuthenticationHeader(credentials));

        return connection;
    }

    // This is just to avoid putting password chars directly into a string
    private static String getAuthenticationHeader(RedisCredentials credentials) throws IOException {
        // Build Basic auth without creating a password String
        final char[] pass = credentials.getPassword() != null ? credentials.getPassword() : new char[0];
        final String user = credentials.getUser() != null ? credentials.getUser() : "";
        final byte[] userBytes = user.getBytes(StandardCharsets.UTF_8);

        // Encode char[] directly to UTF-8 bytes
        java.nio.ByteBuffer bb = StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(pass));
        byte[] passBytes = new byte[bb.remaining()];
        bb.get(passBytes);

        // user ":" password
        byte[] combined = new byte[userBytes.length + 1 + passBytes.length];
        System.arraycopy(userBytes, 0, combined, 0, userBytes.length);
        combined[userBytes.length] = (byte) ':';
        System.arraycopy(passBytes, 0, combined, userBytes.length + 1, passBytes.length);

        String encodedAuth = Base64.getEncoder().encodeToString(combined);

        // Clear sensitive buffers
        java.util.Arrays.fill(passBytes, (byte) 0);
        java.util.Arrays.fill(combined, (byte) 0);
        return "Basic " + encodedAuth;
    }

    /**
     * Parses the response body and extracts the list of UIDs.
     */
    static List<String> parseUidsFromResponse(String responseBody) {
        return JsonParser.parseString(responseBody).getAsJsonArray().asList().stream().map(e -> {
            if (e.isJsonObject() && e.getAsJsonObject().has("uid")) {
                return e.getAsJsonObject().get("uid").getAsString();
            }
            return e.getAsString();
        }).filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList());
    }

    static String readResponse(HttpURLConnection connection) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            if (inputStream == null) {
                inputStream = connection.getErrorStream();
            }
        } catch (IOException e) {
            // If there's an error, try to read from error stream
            inputStream = connection.getErrorStream();
        }
        if (inputStream == null) {
            throw new IOException(
                "No response stream available from server (code=" + connection.getResponseCode() + ")");
        }

        StringBuilder response = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            response.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }

        inputStream.close();
        return response.toString();
    }

}
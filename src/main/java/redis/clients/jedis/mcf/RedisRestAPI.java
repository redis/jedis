package redis.clients.jedis.mcf;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.RedisCredentials;

/**
 * Helper class to check the availability of a Redis database
 */
class RedisRestAPI {

    private static final Logger log = LoggerFactory.getLogger(RedisRestAPI.class);
    private static final String BDBS_URL = "https://%s:%s/v1/bdbs?fields=uid,endpoints";
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

    public List<RedisRestAPI.BdbInfo> getBdbs() throws IOException {
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
            return parseBdbInfoFromResponse(responseBody);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public boolean checkBdbAvailability(String uid, boolean lagAware) throws IOException {
        return checkBdbAvailability(uid, lagAware, null);
    }

    public boolean checkBdbAvailability(String uid, boolean extendedCheckEnabled, Long availabilityLagToleranceMs)
        throws IOException {
        String availabilityUri;
        if (extendedCheckEnabled) {
            // Use extended check with lag validation
            availabilityUri = String.format(LAGAWARE_AVAILABILITY_URL, endpoint.getHost(), endpoint.getPort(), uid);
            if (availabilityLagToleranceMs != null) {
                availabilityUri = availabilityUri + "&availability_lag_tolerance_ms=" + availabilityLagToleranceMs;
            }
        } else {
            // Use standard datapath validation only
            availabilityUri = String.format(AVAILABILITY_URL, endpoint.getHost(), endpoint.getPort(), uid);
        }

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
     * Parses the response body and extracts BDB information including endpoints.
     * @param responseBody the JSON response containing BDBs with endpoints
     * @return list of BDB information objects
     */
    static List<RedisRestAPI.BdbInfo> parseBdbInfoFromResponse(String responseBody) {
        JsonArray bdbs = JsonParser.parseString(responseBody).getAsJsonArray();
        List<RedisRestAPI.BdbInfo> bdbInfoList = new ArrayList<>();

        for (JsonElement bdbElement : bdbs) {
            if (!bdbElement.isJsonObject()) {
                continue;
            }

            JsonObject bdb = bdbElement.getAsJsonObject();
            if (!bdb.has("uid")) {
                continue;
            }

            String bdbId = bdb.get("uid").getAsString();
            List<RedisRestAPI.EndpointInfo> endpoints = new ArrayList<>();

            if (bdb.has("endpoints") && bdb.get("endpoints").isJsonArray()) {
                JsonArray endpointsArray = bdb.getAsJsonArray("endpoints");

                for (JsonElement endpointElement : endpointsArray) {
                    if (!endpointElement.isJsonObject()) {
                        continue;
                    }

                    JsonObject endpoint = endpointElement.getAsJsonObject();

                    // Extract addr array
                    List<String> addrList = new ArrayList<>();
                    if (endpoint.has("addr") && endpoint.get("addr").isJsonArray()) {
                        JsonArray addresses = endpoint.getAsJsonArray("addr");
                        for (JsonElement addrElement : addresses) {
                            if (addrElement.isJsonPrimitive()) {
                                addrList.add(addrElement.getAsString());
                            }
                        }
                    }

                    // Extract other fields
                    String dnsName = endpoint.has("dns_name") ? endpoint.get("dns_name").getAsString() : null;
                    Integer port = endpoint.has("port") ? endpoint.get("port").getAsInt() : null;
                    String endpointUid = endpoint.has("uid") ? endpoint.get("uid").getAsString() : null;

                    endpoints.add(new RedisRestAPI.EndpointInfo(addrList, dnsName, port, endpointUid));
                }
            }

            bdbInfoList.add(new RedisRestAPI.BdbInfo(bdbId, endpoints));
        }

        return bdbInfoList;
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

    /**
     * Information about a Redis Enterprise BDB (database) including its endpoints.
     */
    static class BdbInfo {
        private final String uid;
        private final List<EndpointInfo> endpoints;

        BdbInfo(String uid, List<EndpointInfo> endpoints) {
            this.uid = uid;
            this.endpoints = endpoints;
        }

        String getUid() {
            return uid;
        }

        List<EndpointInfo> getEndpoints() {
            return endpoints;
        }

        /**
         * Find the BDB that matches the given database host by comparing endpoints.
         * @param bdbs list of BDB information
         * @param dbHost the database host to match
         * @return the matching BDB, or null if no match is found
         */
        static BdbInfo findMatchingBdb(List<BdbInfo> bdbs, String dbHost) {
            for (BdbInfo bdb : bdbs) {
                for (EndpointInfo endpoint : bdb.getEndpoints()) {
                    // First check dns_name
                    if (dbHost.equals(endpoint.getDnsName())) {
                        return bdb;
                    }

                    // Then check addr array for IP addresses
                    if (endpoint.getAddr() != null) {
                        for (String addr : endpoint.getAddr()) {
                            if (dbHost.equals(addr)) {
                                return bdb;
                            }
                        }
                    }
                }
            }
            return null; // No matching BDB found
        }

        @Override
        public String toString() {
            return "BdbInfo{" + "uid='" + uid + '\'' + ", endpoints=" + endpoints + '}';
        }
    }

    /**
     * Information about a Redis Enterprise BDB endpoint.
     */
    static class EndpointInfo {
        private final List<String> addr;
        private final String dnsName;
        private final Integer port;
        private final String uid;

        EndpointInfo(List<String> addr, String dnsName, Integer port, String uid) {
            this.addr = addr;
            this.dnsName = dnsName;
            this.port = port;
            this.uid = uid;
        }

        List<String> getAddr() {
            return addr;
        }

        String getDnsName() {
            return dnsName;
        }

        Integer getPort() {
            return port;
        }

        String getUid() {
            return uid;
        }

        @Override
        public String toString() {
            return "EndpointInfo{" + "addr=" + addr + ", dnsName='" + dnsName + '\'' + ", port=" + port + ", uid='"
                + uid + '\'' + '}';
        }
    }

}
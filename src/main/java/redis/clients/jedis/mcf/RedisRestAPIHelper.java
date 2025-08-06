package redis.clients.jedis.mcf;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Helper class to check the availability of a Redis database
 */
class RedisRestAPIHelper {
    // Required connection information - replace with your host, port, username, and
    // password
    private final String host;
    private final String port;
    private final String userName;
    private final String password;

    RedisRestAPIHelper(String host, String port, String userName, String password) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }

    private static final String BDBS_URL = "https://%s:%s/v1/bdbs";
    private static final String AVAILABILITY_URL = "https://%s:%s/v1/bdbs/%d/availability";

    public List<String> getBdbs() throws IOException {
        String bdbsUri = String.format(BDBS_URL, host, port);
        HttpURLConnection getConnection = createConnection(bdbsUri, "GET");
        getConnection.setRequestProperty("Accept", "application/json");

        String responseBody = readResponse(getConnection);
        return JsonParser.parseString(responseBody).getAsJsonArray().asList().stream().map(e -> e.getAsString())
            .collect(Collectors.toList());
    }

    public String checkBdbAvailability(String uid) throws IOException {
        String availabilityUri = String.format(AVAILABILITY_URL, host, port, uid);
        HttpURLConnection availConnection = createConnection(availabilityUri, "GET");
        availConnection.setRequestProperty("Accept", "application/json");

        String availResponse = readResponse(availConnection);
        JsonObject availJson = JsonParser.parseString(availResponse).getAsJsonObject();

        return availJson.get("status").getAsString();

    }

    private HttpURLConnection createConnection(String urlString, String method) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);

        // Set basic authentication
        String auth = userName + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

        return connection;
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            // If there's an error, try to read from error stream
            inputStream = connection.getErrorStream();
            if (inputStream == null) {
                return "";
            }
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
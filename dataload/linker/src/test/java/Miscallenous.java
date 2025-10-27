import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Miscallenous {


    public static Path resolveConfigFile(String relativePath) {
        // Start from current working directory (module's base)
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path configPath = currentDir.resolve(relativePath);

        // If it doesn't exist, walk up to find "dataload/configs"
        while (currentDir != null && !Files.exists(configPath)) {
            currentDir = currentDir.getParent();
            if (currentDir == null) break;
            configPath = currentDir.resolve("dataload").resolve(relativePath);
        }

        if (!Files.exists(configPath)) {
            throw new RuntimeException("Config file not found: " + configPath.toAbsolutePath());
        }

        return configPath.toAbsolutePath();
    }

    public static boolean isServiceUp(String serviceUrl) {
        try {
            // Ensure the URL ends with a "/" to avoid errors
            if (!serviceUrl.endsWith("/")) {
                serviceUrl += "/";
            }

            URL url = new URL(serviceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000); // 2 seconds timeout
            connection.setReadTimeout(2000);

            int responseCode = connection.getResponseCode();
            // Consider 2xx and 3xx as "up"
            return (responseCode >= 200 && responseCode < 405);

        } catch (IOException e) {
            // Connection refused, timeout, or other I/O exception
            return false;
        }
    }

    public static boolean isFilePresent(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isRegularFile(path);
    }

}

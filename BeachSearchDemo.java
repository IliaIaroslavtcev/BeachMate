import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class BeachSearchDemo {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("=== Testing Beach Search in Benidorm ===");
        
        String[] queries = {
            "benidorm",
            "BENIDORM", 
            "Benidorm",
            "benidorm beach",
            "playa levante",
            "platja llevant"
        };
        
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        for (String query : queries) {
            System.out.println("\n--- Query: '" + query + "' ---");
            testQuery(client, query);
            Thread.sleep(1000); // Rate limiting
        }
    }
    
    private static void testQuery(HttpClient client, String query) throws IOException, InterruptedException {
        // Test both original query and with "beach" added
        String originalQuery = query;
        String beachQuery = query.toLowerCase() + (query.toLowerCase().contains("beach") ? "" : " beach");
        
        System.out.println("Original: " + testSingleQuery(client, originalQuery));
        System.out.println("With beach: " + testSingleQuery(client, beachQuery));
    }
    
    private static String testSingleQuery(HttpClient client, String query) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?q=" + encodedQuery + "&format=json&limit=3";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Beach-Test/1.0")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
            
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            String body = response.body();
            if (body.startsWith("[{")) {
                // Extract first result display_name
                int displayNameStart = body.indexOf("\"display_name\":\"");
                if (displayNameStart > 0) {
                    displayNameStart += 16;
                    int displayNameEnd = body.indexOf("\"", displayNameStart);
                    if (displayNameEnd > displayNameStart) {
                        return body.substring(displayNameStart, displayNameEnd);
                    }
                }
            }
            return "No results";
        } else {
            return "Error " + response.statusCode();
        }
    }
}
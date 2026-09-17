/// Çalışan Student API'ye sağlık ve istatistik isteği atar.
///
/// Derleme veya proje kurulumu gerekmez:
/// `java scripts/HealthCheck.java [http://localhost:8080]`
///
/// Java 25 compact source file: sınıf bildirimi yok, `java.base` otomatik içe aktarılır,
/// HTTP istemcisi için `java.net.http` modülü tek satırda içe aktarılır.
import module java.net.http;

void main(String[] args) throws Exception {
    var baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
    try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()) {
        for (var path : List.of("/api/health", "/api/stats")) {
            var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            IO.println("%s -> HTTP %d%n%s%n".formatted(path, response.statusCode(), response.body()));
        }
    }
}

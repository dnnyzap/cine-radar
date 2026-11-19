package io.github.dnnyzap.cineradar;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP simples e educado: User-Agent fixo, timeout, uma nova tentativa em erro de rede
 * e uma pausa curta entre requisições para não sobrecarregar os sites.
 */
public class Http {

    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; CineRadar/0.1; +https://github.com/dnnyzap/cine-radar)";

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final long pausaMs;

    public Http(long pausaMs) {
        this.pausaMs = pausaMs;
    }

    /** Resultado de uma requisição: status HTTP e corpo. */
    public record Resposta(int status, String corpo) {
        public boolean ok() { return status >= 200 && status < 300; }
    }

    public Resposta get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                .timeout(Duration.ofSeconds(30))
                .GET().build();
        return enviar(req);
    }

    public Resposta postJson(String url, String json) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return enviar(req);
    }

    private Resposta enviar(HttpRequest req) throws IOException, InterruptedException {
        IOException ultimoErro = null;
        for (int tentativa = 1; tentativa <= 2; tentativa++) {
            try {
                Thread.sleep(pausaMs);
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                return new Resposta(resp.statusCode(), resp.body());
            } catch (IOException e) {
                ultimoErro = e;
                Thread.sleep(2000L * tentativa);
            }
        }
        throw ultimoErro;
    }
}

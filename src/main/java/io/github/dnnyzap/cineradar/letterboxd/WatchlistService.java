package io.github.dnnyzap.cineradar.letterboxd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dnnyzap.cineradar.Http;
import io.github.dnnyzap.cineradar.model.Filme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Busca a watchlist. Primeiro tenta raspar a página pública do Letterboxd;
 * se falhar (perfil privado, bloqueio etc.), usa o CSV do export, se existir.
 *
 * Também enriquece cada filme com títulos alternativos (título original e,
 * opcionalmente, o título em pt-BR via TMDB), guardados em cache entre execuções.
 */
public class WatchlistService {

    private static final int MAX_PAGINAS = 30;
    /** Limite de páginas de filme consultadas por execução, para ser educado com o site. */
    private static final int MAX_ENRIQUECIMENTOS_POR_EXECUCAO = 60;

    private final Http http;
    private final Path csvReserva;
    private final String tmdbApiKey;
    private final ObjectMapper json = new ObjectMapper();

    public WatchlistService(Http http, Path csvReserva, String tmdbApiKey) {
        this.http = http;
        this.csvReserva = csvReserva;
        this.tmdbApiKey = tmdbApiKey == null || tmdbApiKey.isBlank() ? null : tmdbApiKey.trim();
    }

    public List<Filme> buscar(String usuario) throws Exception {
        if (usuario != null && !usuario.isBlank()) {
            try {
                List<Filme> filmes = raspar(usuario.trim());
                if (!filmes.isEmpty()) {
                    System.out.printf("Watchlist de %s: %d filmes (Letterboxd)%n", usuario, filmes.size());
                    return filmes;
                }
                System.out.println("Watchlist veio vazia (perfil privado?). Tentando o CSV...");
            } catch (Exception e) {
                System.out.println("Não consegui ler a watchlist no Letterboxd: " + e.getMessage());
            }
        }
        if (csvReserva != null && Files.exists(csvReserva)) {
            List<Filme> filmes = WatchlistCsv.ler(csvReserva);
            System.out.printf("Watchlist: %d filmes (%s)%n", filmes.size(), csvReserva);
            return filmes;
        }
        throw new IllegalStateException(
                "Sem watchlist: configure letterboxdUsuario ou coloque o export em " + csvReserva);
    }

    private List<Filme> raspar(String usuario) throws Exception {
        List<Filme> todos = new ArrayList<>();
        for (int pagina = 1; pagina <= MAX_PAGINAS; pagina++) {
            String url = "https://letterboxd.com/" + usuario + "/watchlist/"
                    + (pagina > 1 ? "page/" + pagina + "/" : "");
            Http.Resposta r = http.get(url);
            if (!r.ok()) throw new IllegalStateException("HTTP " + r.status() + " em " + url);
            LetterboxdParser.PaginaWatchlist p = LetterboxdParser.parseWatchlist(r.corpo());
            todos.addAll(p.filmes());
            if (!p.temProximaPagina() || p.filmes().isEmpty()) break;
        }
        return todos;
    }

    /**
     * Adiciona títulos alternativos aos filmes. O cache (slug → títulos) é alterado no lugar
     * e deve ser salvo pelo chamador.
     */
    public List<Filme> enriquecer(List<Filme> filmes, Map<String, List<String>> cache) {
        int consultas = 0;
        List<Filme> resultado = new ArrayList<>();
        for (Filme f : filmes) {
            List<String> extras = cache.get(f.slug());
            if (extras == null && consultas < MAX_ENRIQUECIMENTOS_POR_EXECUCAO && !f.slug().isBlank()) {
                consultas++;
                extras = buscarTitulosAlternativos(f);
                if (extras != null) cache.put(f.slug(), extras);
            }
            resultado.add(extras == null ? f : f.comTitulosExtras(extras));
        }
        if (consultas > 0) System.out.printf("Títulos alternativos buscados para %d filmes%n", consultas);
        return resultado;
    }

    /** Retorna null em caso de erro (tenta de novo na próxima execução). */
    private List<String> buscarTitulosAlternativos(Filme f) {
        try {
            Http.Resposta r = http.get(f.link());
            if (!r.ok()) return r.status() == 404 ? List.of() : null;
            LetterboxdParser.DetalhesFilme d = LetterboxdParser.parseFilme(r.corpo());
            List<String> extras = new ArrayList<>();
            if (d.tituloOriginal() != null) extras.add(d.tituloOriginal());
            if (tmdbApiKey != null && d.tmdbId() != null) {
                String ptBr = tituloTmdbPtBr(d.tmdbId());
                if (ptBr != null) extras.add(ptBr);
            }
            return extras;
        } catch (Exception e) {
            return null;
        }
    }

    private String tituloTmdbPtBr(String tmdbId) throws Exception {
        Http.Resposta r = http.get("https://api.themoviedb.org/3/movie/" + tmdbId
                + "?language=pt-BR&api_key=" + tmdbApiKey);
        if (!r.ok()) return null;
        JsonNode n = json.readTree(r.corpo());
        String titulo = n.path("title").asText(null);
        return titulo == null || titulo.isBlank() ? null : titulo;
    }
}

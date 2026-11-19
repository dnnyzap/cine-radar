package io.github.dnnyzap.cineradar.adorocinema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dnnyzap.cineradar.model.Sessao;
import io.github.dnnyzap.cineradar.model.Sessao.Versao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lê o JSON de programação do AdoroCinema (/_/showtimes/theater-{id}/d-{data}/p-{pagina}/).
 *
 * Formato (resumido):
 * <pre>
 * { "pagination": {"page":1,"totalPages":1},
 *   "results": [ { "movie": {"title":..., "originalTitle":..., "data":{"productionYear":2026}},
 *                  "showtimes": { "dubbed":[...], "original":[...], "local":[...], "*_st":[...] } } ] }
 * </pre>
 * Cada sessão tem internalId, startsAt, diffusionVersion (ORIGINAL = legendado), tags e links de ingresso.
 */
public final class AdoroCinemaParser {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Tags do AdoroCinema que viram "formato" na notificação. */
    private static final Map<String, String> TAGS_FORMATO = Map.of(
            "Showtime.Service.VIP", "VIP",
            "Auditorium.Experience.DolbyAtmos", "Dolby Atmos",
            "Auditorium.Experience.PLF", "Sala Premium",
            "Showtime.Accessibility.Subtitled", "Legenda acessível",
            "Showtime.Accessibility.MovieReading", "Audiodescrição");

    private AdoroCinemaParser() {}

    public record Pagina(List<Sessao> sessoes, int pagina, int totalPaginas) {}

    public static Pagina parse(String corpo, String cinemaId, String cinemaNome) throws IOException {
        JsonNode raiz = JSON.readTree(corpo);
        List<Sessao> sessoes = new ArrayList<>();

        for (JsonNode resultado : raiz.path("results")) {
            JsonNode filme = resultado.path("movie");
            String titulo = filme.path("title").asText("");
            String original = filme.path("originalTitle").asText(titulo);
            JsonNode anoNode = filme.path("data").path("productionYear");
            Integer ano = anoNode.isInt() ? anoNode.asInt() : null;

            Iterator<Map.Entry<String, JsonNode>> grupos = resultado.path("showtimes").fields();
            while (grupos.hasNext()) {
                for (JsonNode s : grupos.next().getValue()) {
                    String inicio = s.path("startsAt").asText(null);
                    if (inicio == null) continue;
                    sessoes.add(new Sessao(
                            s.path("internalId").asLong(),
                            cinemaId,
                            cinemaNome,
                            titulo,
                            original,
                            ano,
                            LocalDateTime.parse(inicio.length() > 19 ? inicio.substring(0, 19) : inicio),
                            Versao.deAdoroCinema(s.path("diffusionVersion").asText(null)),
                            formatos(s),
                            linkIngresso(s)));
                }
            }
        }

        JsonNode pag = raiz.path("pagination");
        return new Pagina(sessoes, pag.path("page").asInt(1), pag.path("totalPages").asInt(1));
    }

    static Set<String> formatos(JsonNode sessao) {
        Set<String> formatos = new LinkedHashSet<>();
        for (JsonNode t : sessao.path("tags")) {
            String tag = t.asText("");
            if (tag.startsWith("Format.Projection.")) {
                String nome = tag.substring("Format.Projection.".length());
                if (!nome.equals("Digital") && !nome.equals("Scope")) {
                    formatos.add(nome.equalsIgnoreCase("MacroXE") ? "Macro XE" : nome.toUpperCase());
                }
            } else if (TAGS_FORMATO.containsKey(tag)) {
                formatos.add(TAGS_FORMATO.get(tag));
            } else if (tag.toUpperCase().contains("IMAX")) {
                formatos.add("IMAX");
            }
        }
        return formatos;
    }

    /** Prefere o link da Ingresso.com; se não houver, usa o primeiro disponível. */
    static String linkIngresso(JsonNode sessao) {
        String primeiro = null;
        for (JsonNode t : sessao.path("data").path("ticketing")) {
            JsonNode urls = t.path("urls");
            if (urls.isEmpty()) continue;
            String url = urls.get(0).asText();
            if ("ingresso".equals(t.path("provider").asText())) return url;
            if (primeiro == null) primeiro = url;
        }
        return primeiro;
    }
}

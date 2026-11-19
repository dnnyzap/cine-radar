package io.github.dnnyzap.cineradar.ingresso;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dnnyzap.cineradar.model.Sessao;
import io.github.dnnyzap.cineradar.model.Sessao.Fonte;
import io.github.dnnyzap.cineradar.model.Sessao.Versao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lê o JSON de sessões do Ingresso.com (api-content.ingresso.com/v0/sessions/city/{cidade}/theater/{cinema}).
 *
 * Formato (resumido): uma lista de dias, cada um com filmes, salas e sessões.
 * <pre>
 * [ { "date": "2026-11-26",
 *     "movies": [ { "title":..., "originalTitle":..., "releaseYear":2026,
 *                   "rooms": [ { "sessions": [ { "id":"87690427", "type":["Normal","Legendado"],
 *                                                 "date":{"localDate":"2026-11-26T14:40:00-04:00"},
 *                                                 "siteURL":"https://checkout.ingresso.com/...",
 *                                                 "enabled":true } ] } ] } ] } ]
 * </pre>
 * Uma chamada traz todos os dias com programação, inclusive a pré-venda.
 */
public final class IngressoParser {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Tipos que indicam a versão (o resto vira formato). */
    private static final Map<String, Versao> VERSOES = Map.of(
            "LEGENDADO", Versao.LEGENDADO,
            "DUBLADO", Versao.DUBLADO,
            "NACIONAL", Versao.NACIONAL);

    /** Nomes alinhados com os do AdoroCinema, para os filtros de formato valerem nas duas fontes. */
    private static final Map<String, String> FORMATOS = Map.of(
            "VIP", "VIP",
            "TELAS GIGANTES/PLF", "Sala Premium",
            "MACRO XE", "Macro XE");

    private IngressoParser() {}

    public static List<Sessao> parse(String corpo, String cinemaId, String cinemaNome) throws IOException {
        JsonNode raiz = JSON.readTree(corpo);
        List<Sessao> sessoes = new ArrayList<>();

        for (JsonNode dia : raiz) {
            for (JsonNode filme : dia.path("movies")) {
                String titulo = filme.path("title").asText("");
                String original = filme.path("originalTitle").asText("");
                if (original.isBlank()) original = titulo;
                JsonNode anoNode = filme.path("releaseYear");
                Integer ano = anoNode.canConvertToInt() && anoNode.asInt() > 0 ? anoNode.asInt() : null;

                for (JsonNode sala : filme.path("rooms")) {
                    for (JsonNode s : sala.path("sessions")) {
                        if (!s.path("enabled").asBoolean(true)) continue;
                        String inicio = s.path("date").path("localDate").asText(null);
                        if (inicio == null || inicio.length() < 16) continue;

                        Versao versao = Versao.DESCONHECIDA;
                        Set<String> formatos = new LinkedHashSet<>();
                        for (JsonNode t : s.path("type")) {
                            String tipo = t.asText("").trim();
                            String chave = tipo.toUpperCase();
                            if (VERSOES.containsKey(chave)) {
                                versao = VERSOES.get(chave);
                            } else if (!tipo.isEmpty() && !chave.equals("NORMAL") && !chave.equals("2D")) {
                                formatos.add(FORMATOS.getOrDefault(chave, tipo));
                            }
                        }

                        String link = s.path("siteURL").asText("");
                        sessoes.add(new Sessao(
                                s.path("id").asLong(),
                                cinemaId,
                                cinemaNome,
                                titulo,
                                original,
                                ano,
                                LocalDateTime.parse(inicio.length() > 19 ? inicio.substring(0, 19) : inicio),
                                versao,
                                formatos,
                                link.isBlank() ? null : link,
                                Fonte.INGRESSO));
                    }
                }
            }
        }
        return sessoes;
    }
}

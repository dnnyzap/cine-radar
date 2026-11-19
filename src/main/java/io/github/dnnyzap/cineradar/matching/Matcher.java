package io.github.dnnyzap.cineradar.matching;

import io.github.dnnyzap.cineradar.config.Config;
import io.github.dnnyzap.cineradar.model.Filme;
import io.github.dnnyzap.cineradar.model.Sessao;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cruza as sessões com a watchlist e aplica as preferências do usuário.
 *
 * A watchlist do Letterboxd usa o título em inglês/original; o cinema usa o título em português.
 * Por isso comparamos qualquer título conhecido do filme com o título original e o título pt-BR
 * da sessão, depois de normalizar (sem acentos, pontuação ou maiúsculas), e conferimos o ano (±1).
 */
public final class Matcher {

    private Matcher() {}

    static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replace("&", " and ")
                .replaceAll("[^a-z0-9\\p{L}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    static boolean mesmoFilme(Filme filme, Sessao sessao) {
        if (filme.ano() != null && sessao.ano() != null && Math.abs(filme.ano() - sessao.ano()) > 1) {
            return false;
        }
        Set<String> doFilme = Stream.concat(Stream.of(filme.titulo()), filme.titulosExtras().stream())
                .map(Matcher::normalizar).filter(t -> !t.isEmpty()).collect(Collectors.toSet());
        return doFilme.contains(normalizar(sessao.tituloOriginal()))
                || doFilme.contains(normalizar(sessao.tituloPtBr()));
    }

    static boolean atendePreferencias(Sessao s, Config cfg, LocalDateTime agora) {
        if (!s.inicio().isAfter(agora)) return false;
        if (!cfg.versoes().contains(s.versao())) return false;

        LocalTime minimo = cfg.horarioMinimoOuNull();
        if (minimo != null && s.inicio().toLocalTime().isBefore(minimo)) return false;

        if (!cfg.formatos().isEmpty()) {
            Set<String> desejados = cfg.formatos().stream().map(String::toUpperCase).collect(Collectors.toSet());
            Set<String> daSessao = s.formatos().stream().map(String::toUpperCase).collect(Collectors.toSet());
            // "2D" = sala comum, sem 3D/IMAX/Macro XE
            boolean salaComum = daSessao.stream().noneMatch(f -> f.equals("3D") || f.equals("IMAX") || f.equals("MACRO XE"));
            boolean ok = (desejados.contains("2D") && salaComum)
                    || daSessao.stream().anyMatch(desejados::contains);
            if (!ok) return false;
        }
        return true;
    }

    /** Resultado: para cada filme da watchlist que tem sessões, as sessões que atendem às preferências. */
    public static Map<Filme, List<Sessao>> cruzar(List<Filme> watchlist, List<Sessao> sessoes,
                                                   Config cfg, LocalDateTime agora) {
        Map<Filme, List<Sessao>> resultado = new LinkedHashMap<>();
        for (Sessao s : sessoes) {
            if (!atendePreferencias(s, cfg, agora)) continue;
            for (Filme f : watchlist) {
                if (mesmoFilme(f, s)) {
                    resultado.computeIfAbsent(f, k -> new ArrayList<>()).add(s);
                    break;
                }
            }
        }
        resultado.values().forEach(l -> l.sort((a, b) -> a.inicio().compareTo(b.inicio())));
        return resultado;
    }
}

package io.github.dnnyzap.cineradar.ingresso;

import io.github.dnnyzap.cineradar.Http;
import io.github.dnnyzap.cineradar.config.Config.Cinema;
import io.github.dnnyzap.cineradar.model.Sessao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Baixa a programação de cada cinema pela API de conteúdo do Ingresso.com (uma chamada por cinema). */
public class IngressoClient {

    private static final String BASE = "https://api-content.ingresso.com/v0/sessions/city/%s/theater/%s?partnership=home";

    private final Http http;

    public IngressoClient(Http http) {
        this.http = http;
    }

    /** Sessões de {@code inicio} até {@code inicio + dias - 1}. */
    public List<Sessao> buscar(String cidade, List<Cinema> cinemas, LocalDate inicio, int dias) {
        List<Sessao> todas = new ArrayList<>();
        LocalDate fim = inicio.plusDays(dias);
        for (Cinema cinema : cinemas) {
            if (cinema.ingresso() == null || cinema.ingresso().isBlank()) continue;
            try {
                Http.Resposta r = http.get(BASE.formatted(cidade, cinema.ingresso()));
                if (!r.ok()) throw new IllegalStateException("HTTP " + r.status());
                List<Sessao> doCinema = IngressoParser.parse(r.corpo(), cinema.chave(), cinema.nome()).stream()
                        .filter(s -> !s.inicio().toLocalDate().isBefore(inicio) && s.inicio().toLocalDate().isBefore(fim))
                        .toList();
                todas.addAll(doCinema);
                System.out.printf("Ingresso.com · %s: %d sessões%n", cinema.nome(), doCinema.size());
            } catch (Exception e) {
                System.out.printf("  ! Ingresso.com · %s: %s%n", cinema.nome(), e.getMessage());
            }
        }
        return todas;
    }
}

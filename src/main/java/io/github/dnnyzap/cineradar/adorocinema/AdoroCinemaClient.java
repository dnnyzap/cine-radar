package io.github.dnnyzap.cineradar.adorocinema;

import io.github.dnnyzap.cineradar.Http;
import io.github.dnnyzap.cineradar.config.Config.Cinema;
import io.github.dnnyzap.cineradar.model.Sessao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Baixa a programação de cada cinema, dia a dia, pelo endpoint JSON do AdoroCinema. */
public class AdoroCinemaClient {

    private static final String BASE = "https://www.adorocinema.com/_/showtimes/theater-%s/d-%s/p-%d/";
    private static final int MAX_PAGINAS = 10;

    private final Http http;

    public AdoroCinemaClient(Http http) {
        this.http = http;
    }

    public List<Sessao> buscar(List<Cinema> cinemas, LocalDate inicio, int dias) {
        List<Sessao> todas = new ArrayList<>();
        for (Cinema cinema : cinemas) {
            int antes = todas.size();
            for (int d = 0; d < dias; d++) {
                LocalDate data = inicio.plusDays(d);
                try {
                    todas.addAll(buscarDia(cinema, data));
                } catch (Exception e) {
                    System.out.printf("  ! %s em %s: %s%n", cinema.nome(), data, e.getMessage());
                }
            }
            System.out.printf("%s: %d sessões%n", cinema.nome(), todas.size() - antes);
        }
        return todas;
    }

    private List<Sessao> buscarDia(Cinema cinema, LocalDate data) throws Exception {
        List<Sessao> sessoes = new ArrayList<>();
        for (int p = 1; p <= MAX_PAGINAS; p++) {
            Http.Resposta r = http.get(BASE.formatted(cinema.id(), data, p));
            if (!r.ok()) throw new IllegalStateException("HTTP " + r.status());
            AdoroCinemaParser.Pagina pagina = AdoroCinemaParser.parse(r.corpo(), cinema.id(), cinema.nome());
            sessoes.addAll(pagina.sessoes());
            if (pagina.pagina() >= pagina.totalPaginas()) break;
        }
        return sessoes;
    }
}

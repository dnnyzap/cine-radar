package io.github.dnnyzap.cineradar.matching;

import io.github.dnnyzap.cineradar.config.Config;
import io.github.dnnyzap.cineradar.model.Filme;
import io.github.dnnyzap.cineradar.model.Sessao;
import io.github.dnnyzap.cineradar.model.Sessao.Versao;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatcherTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 19, 12, 0);

    private static Sessao sessao(String ptBr, String original, Integer ano, Versao v, Set<String> formatos, int hora) {
        return new Sessao(hora, "F0281", "Kinoplex", ptBr, original, ano,
                LocalDateTime.of(2026, 11, 20, hora, 0), v, formatos, null);
    }

    private static Config cfg(Set<Versao> versoes, Set<String> formatos, String horaMin) {
        return new Config("x", 7, versoes, formatos, horaMin, List.of());
    }

    @Test
    void normalizaAcentosPontuacaoEMaiusculas() {
        assertEquals("ainda estou aqui", Matcher.normalizar("  Ainda Estou Aqui! "));
        assertEquals("o auto da compadecida 2", Matcher.normalizar("O Auto da Compadecida: 2"));
        assertEquals("fast and furious", Matcher.normalizar("Fast & Furious"));
    }

    @Test
    void casaPeloTituloOriginal() {
        Filme f = new Filme("i-play-rocky", "I Play Rocky", 2026);
        assertTrue(Matcher.mesmoFilme(f, sessao("Eu Sou o Rocky", "I Play Rocky", 2026, Versao.LEGENDADO, Set.of(), 20)));
    }

    @Test
    void casaPeloTituloAlternativoDoCache() {
        Filme f = new Filme("parasite-2019", "Parasite", 2019, List.of("기생충", "Parasita"));
        assertTrue(Matcher.mesmoFilme(f, sessao("Parasita", "Gisaengchung", 2019, Versao.LEGENDADO, Set.of(), 20)));
    }

    @Test
    void anoDiferenteNaoCasaMesmoComTituloIgual() {
        Filme remake = new Filme("dune-2021", "Dune", 2021);
        assertFalse(Matcher.mesmoFilme(remake, sessao("Duna", "Dune", 1984, Versao.LEGENDADO, Set.of(), 20)));
        // Diferença de 1 ano é tolerada (lançamento no Brasil costuma ser no ano seguinte)
        assertTrue(Matcher.mesmoFilme(remake, sessao("Duna", "Dune", 2020, Versao.LEGENDADO, Set.of(), 20)));
    }

    @Test
    void filtraVersaoHorarioEFormato() {
        Config soLegendadoNoite = cfg(Set.of(Versao.LEGENDADO), Set.of(), "18:00");
        assertTrue(Matcher.atendePreferencias(sessao("A", "A", 2026, Versao.LEGENDADO, Set.of(), 20), soLegendadoNoite, AGORA));
        assertFalse(Matcher.atendePreferencias(sessao("A", "A", 2026, Versao.DUBLADO, Set.of(), 20), soLegendadoNoite, AGORA));
        assertFalse(Matcher.atendePreferencias(sessao("A", "A", 2026, Versao.LEGENDADO, Set.of(), 14), soLegendadoNoite, AGORA));

        Config soImax = cfg(Set.of(Versao.LEGENDADO), Set.of("imax"), null);
        assertTrue(Matcher.atendePreferencias(sessao("A", "A", 2026, Versao.LEGENDADO, Set.of("IMAX"), 20), soImax, AGORA));
        assertFalse(Matcher.atendePreferencias(sessao("A", "A", 2026, Versao.LEGENDADO, Set.of(), 20), soImax, AGORA));

        Config so2d = cfg(Set.of(Versao.LEGENDADO), Set.of("2D"), null);
        assertTrue(Matcher.atendePreferencias(sessao("A", "A", 2026, Versao.LEGENDADO, Set.of("VIP"), 20), so2d, AGORA));
        assertFalse(Matcher.atendePreferencias(sessao("A", "A", 2026, Versao.LEGENDADO, Set.of("3D"), 20), so2d, AGORA));
    }

    @Test
    void ignoraSessoesQueJaComecaram() {
        Sessao passada = new Sessao(1, "F", "C", "A", "A", 2026, AGORA.minusMinutes(5), Versao.LEGENDADO, Set.of(), null);
        assertFalse(Matcher.atendePreferencias(passada, cfg(null, null, null), AGORA));
    }

    @Test
    void cruzaWatchlistComSessoesEOrdenaPorHorario() {
        Filme rocky = new Filme("i-play-rocky", "I Play Rocky", 2026);
        Filme outro = new Filme("outro", "Outro Filme", 2026);
        List<Sessao> sessoes = List.of(
                sessao("Eu Sou o Rocky", "I Play Rocky", 2026, Versao.LEGENDADO, Set.of(), 21),
                sessao("Eu Sou o Rocky", "I Play Rocky", 2026, Versao.LEGENDADO, Set.of(), 15),
                sessao("Eu Sou o Rocky", "I Play Rocky", 2026, Versao.DUBLADO, Set.of(), 16),
                sessao("Camisas Negras", "Camisas Negras", 2026, Versao.LEGENDADO, Set.of(), 17));

        Map<Filme, List<Sessao>> r = Matcher.cruzar(List.of(rocky, outro), sessoes, cfg(Set.of(Versao.LEGENDADO), null, null), AGORA);

        assertEquals(Set.of(rocky), r.keySet());
        assertEquals(List.of(15, 21), r.get(rocky).stream().map(s -> s.inicio().getHour()).toList());
    }

    private static Sessao doIngresso(String cinemaId, Versao v, int hora) {
        return new Sessao(1000 + hora, cinemaId, "Cinema", "Tudo Pode Ser Visto", "You Can See Everything", 2026,
                LocalDateTime.of(2026, 11, 26, hora, 0), v, Set.of(), "https://checkout.ingresso.com/x", Sessao.Fonte.INGRESSO);
    }

    private static Sessao doAdoro(String cinemaId, Versao v, int hora) {
        return new Sessao(hora, cinemaId, "Cinema", "Tudo Pode Ser Visto", "You Can See Everything", 2026,
                LocalDateTime.of(2026, 11, 26, hora, 0), v, Set.of(), null);
    }

    @Test
    void ingressoTemPrioridadeSobreAdoroCinemaNaMesmaSessao() {
        Filme f = new Filme("you-can-see-everything", "You Can See Everything", 2026);
        // AdoroCinema diz dublado; Ingresso.com diz legendado para o mesmo cinema e horário.
        List<Sessao> sessoes = List.of(doAdoro("F0793", Versao.DUBLADO, 20), doIngresso("F0793", Versao.LEGENDADO, 20));

        Map<Filme, List<Sessao>> r = Matcher.cruzar(List.of(f), sessoes, cfg(Set.of(Versao.LEGENDADO), null, null), AGORA);
        assertEquals(1, r.get(f).size());
        assertEquals(Sessao.Fonte.INGRESSO, r.get(f).get(0).fonte());

        // E o contrário: AdoroCinema diz legendado, Ingresso.com diz dublado -> não avisa.
        List<Sessao> invertido = List.of(doAdoro("F0793", Versao.LEGENDADO, 20), doIngresso("F0793", Versao.DUBLADO, 20));
        assertTrue(Matcher.cruzar(List.of(f), invertido, cfg(Set.of(Versao.LEGENDADO), null, null), AGORA).isEmpty());
    }

    @Test
    void sessoesQueSoUmaFonteTemSaoMantidas() {
        Filme f = new Filme("you-can-see-everything", "You Can See Everything", 2026);
        List<Sessao> sessoes = List.of(
                doAdoro("F0793", Versao.LEGENDADO, 18),      // só no AdoroCinema
                doIngresso("F0793", Versao.LEGENDADO, 20),   // só no Ingresso.com
                doIngresso("ingresso-1303", Versao.LEGENDADO, 21)); // cinema que só existe no Ingresso.com

        Map<Filme, List<Sessao>> r = Matcher.cruzar(List.of(f), sessoes, cfg(Set.of(Versao.LEGENDADO), null, null), AGORA);
        assertEquals(List.of(18, 20, 21), r.get(f).stream().map(s -> s.inicio().getHour()).toList());
    }
}

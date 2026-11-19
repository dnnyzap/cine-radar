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
}

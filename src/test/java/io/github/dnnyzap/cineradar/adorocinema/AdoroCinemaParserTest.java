package io.github.dnnyzap.cineradar.adorocinema;

import io.github.dnnyzap.cineradar.Fixtures;
import io.github.dnnyzap.cineradar.model.Sessao;
import io.github.dnnyzap.cineradar.model.Sessao.Versao;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdoroCinemaParserTest {

    private List<Sessao> sessoes() throws Exception {
        return AdoroCinemaParser.parse(Fixtures.ler("adorocinema-F0281.json"), "F0281", "Kinoplex Amazonas").sessoes();
    }

    @Test
    void leTodasAsSessoesDeTodosOsGrupos() throws Exception {
        assertEquals(4, sessoes().size());
    }

    @Test
    void preencheFilmeHorarioEVersao() throws Exception {
        Sessao s = sessoes().stream().filter(x -> x.id() == 84819687184L).findFirst().orElseThrow();
        assertEquals("Eu Sou o Rocky", s.tituloPtBr());
        assertEquals("I Play Rocky", s.tituloOriginal());
        assertEquals(2026, s.ano());
        assertEquals(LocalDateTime.of(2026, 11, 20, 13, 0), s.inicio());
        assertEquals(Versao.DUBLADO, s.versao());
        assertTrue(s.formatos().isEmpty(), "sala digital comum não tem formato especial");
        assertTrue(s.linkIngresso().startsWith("https://checkout.ingresso.com/"));
    }

    @Test
    void originalViraLegendadoEFormatosEspeciaisSaoReconhecidos() throws Exception {
        Sessao s = sessoes().stream().filter(x -> x.id() == 90000000001L).findFirst().orElseThrow();
        assertEquals(Versao.LEGENDADO, s.versao());
        assertEquals(Set.of("Macro XE", "VIP"), s.formatos());
        assertEquals("https://relay.example/x", s.linkIngresso(), "sem Ingresso.com, usa o primeiro link");
    }

    @Test
    void lePaginacao() throws Exception {
        var p = AdoroCinemaParser.parse(Fixtures.ler("adorocinema-F0281.json"), "F0281", "x");
        assertEquals(1, p.pagina());
        assertEquals(1, p.totalPaginas());
    }
}

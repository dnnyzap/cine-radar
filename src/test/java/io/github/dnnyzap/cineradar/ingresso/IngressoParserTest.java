package io.github.dnnyzap.cineradar.ingresso;

import io.github.dnnyzap.cineradar.Fixtures;
import io.github.dnnyzap.cineradar.model.Sessao;
import io.github.dnnyzap.cineradar.model.Sessao.Fonte;
import io.github.dnnyzap.cineradar.model.Sessao.Versao;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngressoParserTest {

    private List<Sessao> sessoes() throws Exception {
        return IngressoParser.parse(Fixtures.ler("ingresso-1580.json"), "G05J4", "UCI Manauara Shopping");
    }

    @Test
    void leTodosOsDiasEIgnoraSessoesDesabilitadas() throws Exception {
        assertEquals(3, sessoes().size(), "4 sessões no arquivo, 1 com enabled=false");
    }

    @Test
    void preencheFilmeHorarioVersaoELink() throws Exception {
        Sessao s = sessoes().stream().filter(x -> x.id() == 87690427L).findFirst().orElseThrow();
        assertEquals("Tudo Pode Ser Visto", s.tituloPtBr());
        assertEquals("You Can See Everything", s.tituloOriginal());
        assertEquals(2026, s.ano());
        assertEquals(LocalDateTime.of(2026, 11, 26, 14, 40), s.inicio());
        assertEquals(Versao.LEGENDADO, s.versao());
        assertTrue(s.formatos().isEmpty(), "\"Normal\" é sala comum");
        assertEquals("G05J4", s.cinemaId());
        assertEquals("UCI Manauara Shopping", s.cinemaNome());
        assertEquals(Fonte.INGRESSO, s.fonte());
        assertEquals("https://checkout.ingresso.com/?sessionId=87690427&partnership=home", s.linkIngresso());
    }

    @Test
    void tiposQueNaoSaoVersaoViramFormato() throws Exception {
        Sessao s = sessoes().stream().filter(x -> x.id() == 87408113L).findFirst().orElseThrow();
        assertEquals(Versao.DUBLADO, s.versao());
        assertEquals(Set.of("XPLUS"), s.formatos());
    }
}

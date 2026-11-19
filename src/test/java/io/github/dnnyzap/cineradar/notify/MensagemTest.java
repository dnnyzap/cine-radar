package io.github.dnnyzap.cineradar.notify;

import io.github.dnnyzap.cineradar.model.Filme;
import io.github.dnnyzap.cineradar.model.Sessao;
import io.github.dnnyzap.cineradar.model.Sessao.Versao;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MensagemTest {

    @Test
    void agrupaPorCinemaETipoComHorariosELinks() {
        Filme f = new Filme("i-play-rocky", "I Play Rocky", 2026);
        List<Sessao> sessoes = List.of(
                new Sessao(1, "F0281", "Kinoplex Amazonas", "Eu Sou o Rocky", "I Play Rocky", 2026,
                        LocalDateTime.of(2026, 11, 20, 19, 20), Versao.LEGENDADO, Set.of(), "https://ingresso/1"),
                new Sessao(2, "F0281", "Kinoplex Amazonas", "Eu Sou o Rocky", "I Play Rocky", 2026,
                        LocalDateTime.of(2026, 11, 21, 21, 40), Versao.LEGENDADO, Set.of(), "https://ingresso/2"),
                new Sessao(3, "G05J4", "UCI Manauara <Shopping>", "Eu Sou o Rocky", "I Play Rocky", 2026,
                        LocalDateTime.of(2026, 11, 20, 18, 0), Versao.LEGENDADO, Set.of("IMAX"), null));

        String m = Mensagem.montar(f, sessoes);

        assertTrue(m.startsWith("🎬 <b>I Play Rocky</b> (Eu Sou o Rocky)"), m);
        assertTrue(m.contains("📍 <b>Kinoplex Amazonas · Legendado</b>\nsex 20/11 19h20, sáb 21/11 21h40"), m);
        assertTrue(m.contains("UCI Manauara &lt;Shopping&gt; · Legendado · IMAX"), "escapa HTML: " + m);
        assertTrue(m.contains("<a href=\"https://ingresso/1\">Comprar ingresso</a>"), m);
        assertTrue(m.endsWith("<a href=\"https://letterboxd.com/film/i-play-rocky/\">Ver no Letterboxd</a>"), m);
    }

    @Test
    void naoRepeteTituloQuandoPtBrEIgual() {
        Filme f = new Filme("camisas-negras", "Camisas Negras", 2026);
        Sessao s = new Sessao(1, "F", "C", "Camisas Negras", "Camisas Negras", 2026,
                LocalDateTime.of(2026, 11, 20, 13, 30), Versao.NACIONAL, Set.of(), null);
        assertEquals(0, Mensagem.montar(f, List.of(s)).indexOf("🎬 <b>Camisas Negras</b> está com sessões"));
    }
}

package io.github.dnnyzap.cineradar.notify;

import io.github.dnnyzap.cineradar.model.Filme;
import io.github.dnnyzap.cineradar.model.Sessao;
import io.github.dnnyzap.cineradar.model.Sessao.Versao;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MensagemTest {

    private static Sessao s(String cinema, int dia, int hora, int min, Set<String> formatos, String link) {
        return new Sessao(dia * 10000L + hora * 100 + min, "X", cinema, "Eu Sou o Rocky", "I Play Rocky", 2026,
                LocalDateTime.of(2026, 11, dia, hora, min), Versao.LEGENDADO, formatos, link);
    }

    @Test
    void agrupaPorCinemaETipoComUmaLinhaPorDia() {
        Filme f = new Filme("i-play-rocky", "I Play Rocky", 2026);
        List<Sessao> sessoes = List.of(
                s("Kinoplex Amazonas", 20, 19, 20, Set.of(), "https://ingresso/1"),
                s("Kinoplex Amazonas", 21, 21, 40, Set.of(), "https://ingresso/2"),
                s("UCI Manauara <Shopping>", 20, 18, 0, Set.of("IMAX"), null));

        String m = Mensagem.montar(f, sessoes);

        assertTrue(m.startsWith("🎬 <b>I Play Rocky</b>\n<i>Eu Sou o Rocky</i>\n"), m);
        assertTrue(m.contains("📍 <b>Kinoplex Amazonas · Legendado</b>\n• sex 20/11 — 19:20\n• sáb 21/11 — 21:40\n"), m);
        assertTrue(m.contains("UCI Manauara &lt;Shopping&gt; · Legendado · IMAX"), "escapa HTML: " + m);
        assertTrue(m.contains("🎟 <a href=\"https://ingresso/1\">Comprar ingresso</a>"), m);
        assertTrue(m.endsWith("<a href=\"https://letterboxd.com/film/i-play-rocky/\">Ver no Letterboxd</a>"), m);
    }

    @Test
    void juntaDiasSeguidosComOsMesmosHorarios() {
        List<Sessao> sessoes = new ArrayList<>();
        for (int dia = 26; dia <= 29; dia++) {           // qui a dom: mesma grade
            sessoes.add(s("UCI", dia, 18, 5, Set.of(), null));
            sessoes.add(s("UCI", dia, 14, 40, Set.of(), null));
        }
        sessoes.add(s("UCI", 30, 14, 40, Set.of(), null));  // seg: grade diferente

        assertEquals(List.of("• qui 26/11 a dom 29/11 — 14:40, 18:05", "• seg 30/11 — 14:40"),
                Mensagem.linhasPorDia(sessoes));
    }

    @Test
    void naoJuntaDiasComBuraco() {
        List<Sessao> sessoes = List.of(s("C", 20, 22, 0, Set.of(), null), s("C", 22, 22, 0, Set.of(), null));
        assertEquals(List.of("• sex 20/11 — 22:00", "• dom 22/11 — 22:00"), Mensagem.linhasPorDia(sessoes));
    }

    @Test
    void limitaAsLinhasPorGrupo() {
        List<Sessao> sessoes = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) sessoes.add(s("C", dia, 10 + dia, 0, Set.of(), null));
        String m = Mensagem.montar(new Filme("i-play-rocky", "I Play Rocky", 2026), sessoes);
        assertTrue(m.contains("• e mais 2 datas\n"), m);
        assertFalse(m.contains("sáb 07/11"), m);
    }

    @Test
    void naoRepeteTituloQuandoPtBrEIgual() {
        Filme f = new Filme("camisas-negras", "Camisas Negras", 2026);
        Sessao s = new Sessao(1, "F", "C", "Camisas Negras", "Camisas Negras", 2026,
                LocalDateTime.of(2026, 11, 20, 13, 30), Versao.NACIONAL, Set.of(), null);
        assertTrue(Mensagem.montar(f, List.of(s)).startsWith("🎬 <b>Camisas Negras</b>\n\n📍"));
    }
}

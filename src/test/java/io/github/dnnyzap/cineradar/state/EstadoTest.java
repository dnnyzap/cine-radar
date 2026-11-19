package io.github.dnnyzap.cineradar.state;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 19);

    @Test
    void avisaSoNaPrimeiraVezOuDepoisDeSumirUmaSemana() {
        Estado e = new Estado();
        assertTrue(e.deveAvisar("k", HOJE));
        e.marcarVisto("k", HOJE);
        assertFalse(e.deveAvisar("k", HOJE.plusDays(1)));
        assertFalse(e.deveAvisar("k", HOJE.plusDays(Estado.DIAS_PARA_REAVISAR)));
        assertTrue(e.deveAvisar("k", HOJE.plusDays(Estado.DIAS_PARA_REAVISAR + 1)));
    }

    @Test
    void salvaECarregaDoDisco() throws Exception {
        Path arq = Files.createTempDirectory("radar").resolve("data/estado.json");
        Estado e = new Estado();
        e.marcarVisto("filme|F0281|LEGENDADO|", HOJE);
        e.titulosAlternativos.put("parasite-2019", List.of("기생충"));
        e.salvar(arq);

        Estado lido = Estado.carregar(arq);
        assertEquals("2026-11-19", lido.avisos.get("filme|F0281|LEGENDADO|"));
        assertEquals(List.of("기생충"), lido.titulosAlternativos.get("parasite-2019"));
    }

    @Test
    void esqueceAvisosMuitoAntigos() {
        Estado e = new Estado();
        e.marcarVisto("velho", HOJE.minusDays(40));
        e.marcarVisto("novo", HOJE);
        e.limpar(HOJE);
        assertEquals(List.of("novo"), List.copyOf(e.avisos.keySet()));
    }
}

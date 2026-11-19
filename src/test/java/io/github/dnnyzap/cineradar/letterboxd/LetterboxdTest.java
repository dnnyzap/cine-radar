package io.github.dnnyzap.cineradar.letterboxd;

import io.github.dnnyzap.cineradar.Fixtures;
import io.github.dnnyzap.cineradar.model.Filme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterboxdTest {

    @Test
    void leFilmesEPaginacaoDaWatchlist() throws Exception {
        var pagina = LetterboxdParser.parseWatchlist(Fixtures.ler("letterboxd-watchlist.html"));
        assertEquals(3, pagina.filmes().size());
        Filme parasita = pagina.filmes().get(1);
        assertEquals("parasite-2019", parasita.slug());
        assertEquals("Parasite", parasita.titulo());
        assertEquals(2019, parasita.ano());
        assertTrue(pagina.temProximaPagina());
    }

    @Test
    void ultimaPaginaNaoTemProxima() {
        var pagina = LetterboxdParser.parseWatchlist("<ul><li><div data-item-slug=\"x\" data-item-name=\"X\"></div></li></ul>");
        assertEquals(1, pagina.filmes().size());
        assertNull(pagina.filmes().get(0).ano(), "nome sem (ano) não quebra");
        assertTrue(!pagina.temProximaPagina());
    }

    @Test
    void leTituloOriginalEIdDoTmdbDaPaginaDoFilme() throws Exception {
        var d = LetterboxdParser.parseFilme(Fixtures.ler("letterboxd-filme.html"));
        assertEquals("기생충", d.tituloOriginal());
        assertEquals("496243", d.tmdbId());
    }

    @Test
    void leCsvDoExportRespeitandoAspas() throws Exception {
        List<Filme> filmes = WatchlistCsv.ler(Fixtures.caminho("watchlist.csv"));
        assertEquals(2, filmes.size());
        assertEquals("Crouching Tiger, Hidden Dragon", filmes.get(0).titulo());
        assertEquals(2000, filmes.get(0).ano());
        assertEquals("ainda-estou-aqui-2024", filmes.get(1).slug());
        assertEquals("https://boxd.it/def", filmes.get(1).link(), "usa a URI do export como link");
    }
}

package io.github.dnnyzap.cineradar.letterboxd;

import io.github.dnnyzap.cineradar.model.Filme;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extrai dados do HTML do Letterboxd. Não faz requisições — fácil de testar. */
public final class LetterboxdParser {

    private static final Pattern NOME_COM_ANO = Pattern.compile("^(.*)\\s\\((\\d{4})\\)$");

    private LetterboxdParser() {}

    public record PaginaWatchlist(List<Filme> filmes, boolean temProximaPagina) {}

    /** Página da watchlist: cada pôster é um elemento com data-item-slug e data-item-name="Título (Ano)". */
    public static PaginaWatchlist parseWatchlist(String html) {
        Document doc = Jsoup.parse(html);
        List<Filme> filmes = new ArrayList<>();
        for (Element el : doc.select("[data-item-slug], [data-film-slug]")) {
            String slug = el.hasAttr("data-item-slug") ? el.attr("data-item-slug") : el.attr("data-film-slug");
            String nome = el.hasAttr("data-item-name") ? el.attr("data-item-name") : el.attr("data-film-name");
            if (slug.isBlank() || nome.isBlank()) continue;
            if (filmes.stream().anyMatch(f -> f.slug().equals(slug))) continue;

            Matcher m = NOME_COM_ANO.matcher(nome.trim());
            if (m.matches()) {
                filmes.add(new Filme(slug, m.group(1), Integer.parseInt(m.group(2))));
            } else {
                filmes.add(new Filme(slug, nome.trim(), null));
            }
        }
        boolean proxima = !doc.select(".paginate-nextprev a.next, a.next").isEmpty();
        return new PaginaWatchlist(filmes, proxima);
    }

    public record DetalhesFilme(String tituloOriginal, String tmdbId) {}

    /** Página de um filme: título original (h2.originalname) e id do TMDB (atributo do body). */
    public static DetalhesFilme parseFilme(String html) {
        Document doc = Jsoup.parse(html);
        Element original = doc.selectFirst("h2.originalname, .originalname");
        String tmdb = doc.body() == null ? "" : doc.body().attr("data-tmdb-id");
        return new DetalhesFilme(
                original == null ? null : original.text().trim(),
                tmdb.isBlank() ? null : tmdb);
    }
}

package io.github.dnnyzap.cineradar.model;

import java.util.List;

/**
 * Um filme da watchlist do Letterboxd.
 *
 * @param slug           identificador do Letterboxd (ex.: "parasite-2019")
 * @param titulo         título exibido no Letterboxd (geralmente em inglês)
 * @param ano            ano de lançamento, ou null se desconhecido
 * @param titulosExtras  outros títulos conhecidos (título original, título em pt-BR...)
 * @param url            link do filme no Letterboxd; se null, é montado a partir do slug
 */
public record Filme(String slug, String titulo, Integer ano, List<String> titulosExtras, String url) {

    public Filme {
        titulosExtras = titulosExtras == null ? List.of() : List.copyOf(titulosExtras);
    }

    public Filme(String slug, String titulo, Integer ano) {
        this(slug, titulo, ano, List.of(), null);
    }

    public Filme(String slug, String titulo, Integer ano, List<String> titulosExtras) {
        this(slug, titulo, ano, titulosExtras, null);
    }

    public Filme comTitulosExtras(List<String> extras) {
        return new Filme(slug, titulo, ano, extras, url);
    }

    public String link() {
        return url != null ? url : "https://letterboxd.com/film/" + slug + "/";
    }
}

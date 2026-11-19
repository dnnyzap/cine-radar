package io.github.dnnyzap.cineradar.letterboxd;

import io.github.dnnyzap.cineradar.model.Filme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê o watchlist.csv do export do Letterboxd (Settings → Data → Export your data).
 * Colunas: Date,Name,Year,Letterboxd URI
 */
public final class WatchlistCsv {

    private WatchlistCsv() {}

    public static List<Filme> ler(Path arquivo) throws IOException {
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        List<Filme> filmes = new ArrayList<>();
        if (linhas.isEmpty()) return filmes;

        List<String> cabecalho = dividir(linhas.get(0).replace("﻿", ""));
        int iNome = cabecalho.indexOf("Name");
        int iAno = cabecalho.indexOf("Year");
        int iUri = cabecalho.indexOf("Letterboxd URI");
        if (iNome < 0) throw new IOException("CSV sem coluna 'Name': " + arquivo);

        for (String linha : linhas.subList(1, linhas.size())) {
            if (linha.isBlank()) continue;
            List<String> col = dividir(linha);
            if (col.size() <= iNome) continue;
            String nome = col.get(iNome).trim();
            Integer ano = null;
            if (iAno >= 0 && col.size() > iAno && col.get(iAno).matches("\\d{4}")) {
                ano = Integer.parseInt(col.get(iAno));
            }
            // A URI do export (boxd.it/...) redireciona para a página do filme.
            String uri = iUri >= 0 && col.size() > iUri && col.get(iUri).startsWith("http") ? col.get(iUri).trim() : null;
            filmes.add(new Filme(slugDe(nome, ano), nome, ano, List.of(), uri));
        }
        return filmes;
    }

    /** Divide uma linha CSV respeitando aspas ("a, b" fica num campo só; "" vira "). */
    static List<String> dividir(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"' && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else if (c == '"') {
                    entreAspas = false;
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == ',') {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }

    /** Aproximação do slug do Letterboxd, usada só como identificador interno. */
    static String slugDe(String nome, Integer ano) {
        String base = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return ano == null ? base : base + "-" + ano;
    }
}

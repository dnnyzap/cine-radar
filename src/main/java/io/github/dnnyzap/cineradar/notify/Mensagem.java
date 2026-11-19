package io.github.dnnyzap.cineradar.notify;

import io.github.dnnyzap.cineradar.model.Filme;
import io.github.dnnyzap.cineradar.model.Sessao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Monta o texto (HTML do Telegram) de um aviso.
 *
 * <pre>
 * 🎬 You Can See Everything
 * Tudo Pode Ser Visto
 *
 * 📍 UCI Manauara Shopping · Legendado
 * • qui 26/11 a qua 02/12 — 14:40, 18:05
 * 🎟 Comprar ingresso
 * </pre>
 * Dias seguidos com os mesmos horários viram uma linha só.
 */
public final class Mensagem {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_LINHAS_POR_GRUPO = 5;

    private Mensagem() {}

    /** Agrupa as sessões por cinema + tipo e, dentro de cada grupo, por dia. */
    public static String montar(Filme filme, List<Sessao> sessoes) {
        Sessao primeira = sessoes.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("🎬 <b>").append(esc(filme.titulo())).append("</b>\n");
        if (!normal(filme.titulo()).equals(normal(primeira.tituloPtBr()))) {
            sb.append("<i>").append(esc(primeira.tituloPtBr())).append("</i>\n");
        }

        Map<String, List<Sessao>> grupos = new LinkedHashMap<>();
        for (Sessao s : sessoes) {
            grupos.computeIfAbsent(s.cinemaNome() + " · " + tipo(s), k -> new ArrayList<>()).add(s);
        }
        for (var g : grupos.entrySet()) {
            sb.append("\n📍 <b>").append(esc(g.getKey())).append("</b>\n");
            List<String> linhas = linhasPorDia(g.getValue());
            linhas.stream().limit(MAX_LINHAS_POR_GRUPO).forEach(l -> sb.append(l).append("\n"));
            if (linhas.size() > MAX_LINHAS_POR_GRUPO) {
                int resto = linhas.size() - MAX_LINHAS_POR_GRUPO;
                sb.append("• e mais ").append(resto).append(resto == 1 ? " data\n" : " datas\n");
            }
            String link = g.getValue().get(0).linkIngresso();
            if (link != null) sb.append("🎟 <a href=\"").append(esc(link)).append("\">Comprar ingresso</a>\n");
        }
        sb.append("\n<a href=\"").append(filme.link()).append("\">Ver no Letterboxd</a>");
        return sb.toString();
    }

    /** "• qui 26/11 — 14:40, 18:05", juntando dias seguidos com a mesma grade em "qui 26/11 a qua 02/12". */
    static List<String> linhasPorDia(List<Sessao> sessoes) {
        TreeMap<LocalDate, TreeSet<LocalTime>> porDia = new TreeMap<>();
        for (Sessao s : sessoes) {
            porDia.computeIfAbsent(s.inicio().toLocalDate(), k -> new TreeSet<>()).add(s.inicio().toLocalTime());
        }

        List<String> linhas = new ArrayList<>();
        LocalDate inicio = null, fim = null;
        TreeSet<LocalTime> grade = null;
        for (var e : porDia.entrySet()) {
            boolean continua = fim != null && e.getKey().equals(fim.plusDays(1)) && e.getValue().equals(grade);
            if (continua) {
                fim = e.getKey();
                continue;
            }
            if (inicio != null) linhas.add(linha(inicio, fim, grade));
            inicio = fim = e.getKey();
            grade = e.getValue();
        }
        if (inicio != null) linhas.add(linha(inicio, fim, grade));
        return linhas;
    }

    private static String linha(LocalDate inicio, LocalDate fim, TreeSet<LocalTime> grade) {
        String dias = inicio.equals(fim) ? dia(inicio) : dia(inicio) + " a " + dia(fim);
        String horas = grade.stream().map(h -> h.format(HORA)).collect(Collectors.joining(", "));
        return "• " + dias + " — " + horas;
    }

    static String dia(LocalDate d) {
        String semana = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
        return semana + " " + d.format(DIA);
    }

    static String tipo(Sessao s) {
        String versao = switch (s.versao()) {
            case LEGENDADO -> "Legendado";
            case DUBLADO -> "Dublado";
            case NACIONAL -> "Nacional";
            case DESCONHECIDA -> "Versão não informada";
        };
        return s.formatos().isEmpty() ? versao : versao + " · " + String.join(" · ", s.formatos());
    }

    private static String normal(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

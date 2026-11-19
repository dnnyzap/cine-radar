package io.github.dnnyzap.cineradar.notify;

import io.github.dnnyzap.cineradar.model.Filme;
import io.github.dnnyzap.cineradar.model.Sessao;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Monta o texto (HTML do Telegram) de um aviso. */
public final class Mensagem {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH'h'mm");
    private static final int MAX_HORARIOS_POR_GRUPO = 8;

    private Mensagem() {}

    /** Agrupa as sessões por cinema + tipo e lista os horários. */
    public static String montar(Filme filme, List<Sessao> sessoes) {
        Sessao primeira = sessoes.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("🎬 <b>").append(esc(filme.titulo())).append("</b>");
        if (!normal(filme.titulo()).equals(normal(primeira.tituloPtBr()))) {
            sb.append(" (").append(esc(primeira.tituloPtBr())).append(")");
        }
        sb.append(" está com sessões em Manaus!\n");

        Map<String, List<Sessao>> grupos = new LinkedHashMap<>();
        for (Sessao s : sessoes) {
            grupos.computeIfAbsent(s.cinemaNome() + " · " + tipo(s), k -> new java.util.ArrayList<>()).add(s);
        }
        for (var g : grupos.entrySet()) {
            List<Sessao> lista = g.getValue();
            sb.append("\n📍 <b>").append(esc(g.getKey())).append("</b>\n");
            String horarios = lista.stream().limit(MAX_HORARIOS_POR_GRUPO)
                    .map(Mensagem::horario).collect(Collectors.joining(", "));
            sb.append(horarios);
            if (lista.size() > MAX_HORARIOS_POR_GRUPO) {
                sb.append(" e mais ").append(lista.size() - MAX_HORARIOS_POR_GRUPO);
            }
            sb.append("\n");
            String link = lista.get(0).linkIngresso();
            if (link != null) sb.append("<a href=\"").append(esc(link)).append("\">Comprar ingresso</a>\n");
        }
        sb.append("\n<a href=\"").append(filme.link()).append("\">Ver no Letterboxd</a>");
        return sb.toString();
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

    static String horario(Sessao s) {
        String diaSemana = s.inicio().getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
        return diaSemana + " " + s.inicio().format(DIA) + " " + s.inicio().format(HORA);
    }

    private static String normal(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

package io.github.dnnyzap.cineradar;

import io.github.dnnyzap.cineradar.adorocinema.AdoroCinemaClient;
import io.github.dnnyzap.cineradar.config.Config;
import io.github.dnnyzap.cineradar.ingresso.IngressoClient;
import io.github.dnnyzap.cineradar.letterboxd.WatchlistService;
import io.github.dnnyzap.cineradar.matching.Matcher;
import io.github.dnnyzap.cineradar.model.Filme;
import io.github.dnnyzap.cineradar.model.Sessao;
import io.github.dnnyzap.cineradar.notify.Mensagem;
import io.github.dnnyzap.cineradar.notify.Telegram;
import io.github.dnnyzap.cineradar.state.Estado;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ponto de entrada. Uma execução = buscar watchlist → buscar sessões → cruzar → avisar → salvar estado.
 *
 * Uso: java -jar cine-radar.jar [--dry-run] [--config config.json] [--estado data/estado.json] [--csv data/watchlist.csv]
 */
public class CineRadar {

    private static final ZoneId MANAUS = ZoneId.of("America/Manaus");

    public static void main(String[] args) throws Exception {
        boolean dryRun = false;
        Path config = Path.of("config.json");
        Path estadoArq = Path.of("data/estado.json");
        Path csv = Path.of("data/watchlist.csv");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dry-run" -> dryRun = true;
                case "--config" -> config = Path.of(args[++i]);
                case "--estado" -> estadoArq = Path.of(args[++i]);
                case "--csv" -> csv = Path.of(args[++i]);
                default -> throw new IllegalArgumentException("Argumento desconhecido: " + args[i]);
            }
        }

        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        String chatId = System.getenv("TELEGRAM_CHAT_ID");
        if (!dryRun && (vazio(token) || vazio(chatId))) {
            System.out.println("TELEGRAM_BOT_TOKEN/TELEGRAM_CHAT_ID não definidos: rodando em modo --dry-run.");
            dryRun = true;
        }

        Config cfg = Config.carregar(config);
        Estado estado = Estado.carregar(estadoArq);
        Http http = new Http(800);
        LocalDateTime agora = LocalDateTime.now(MANAUS);
        LocalDate hoje = agora.toLocalDate();

        WatchlistService watchlistService = new WatchlistService(http, csv, System.getenv("TMDB_API_KEY"));
        List<Filme> watchlist = watchlistService.buscar(cfg.usuarioLetterboxd());
        watchlist = watchlistService.enriquecer(watchlist, estado.titulosAlternativos);

        List<Sessao> sessoes = new ArrayList<>(new AdoroCinemaClient(http).buscar(cfg.cinemas(), hoje, cfg.diasAFrente()));
        if (!vazio(cfg.ingressoCidade())) {
            sessoes.addAll(new IngressoClient(http).buscar(cfg.ingressoCidade(), cfg.cinemas(), hoje, cfg.diasAFrente()));
        }
        Map<Filme, List<Sessao>> encontrados = Matcher.cruzar(watchlist, sessoes, cfg, agora);
        System.out.printf("%d filmes da watchlist com sessões nas suas preferências%n", encontrados.size());

        Telegram telegram = dryRun ? null : new Telegram(http, token, chatId);
        int enviados = 0;
        for (var e : encontrados.entrySet()) {
            Filme filme = e.getKey();
            Map<String, List<Sessao>> porChave = new LinkedHashMap<>();
            for (Sessao s : e.getValue()) porChave.computeIfAbsent(chave(filme, s), k -> new ArrayList<>()).add(s);

            List<String> chavesNovas = porChave.keySet().stream().filter(k -> estado.deveAvisar(k, hoje)).toList();
            if (!chavesNovas.isEmpty()) {
                List<Sessao> paraAvisar = chavesNovas.stream().flatMap(k -> porChave.get(k).stream()).sorted(
                        (a, b) -> a.inicio().compareTo(b.inicio())).toList();
                String texto = Mensagem.montar(filme, paraAvisar);
                try {
                    if (dryRun) {
                        System.out.println("\n----- (dry-run) mensagem -----\n" + texto + "\n------------------------------");
                    } else {
                        telegram.enviar(texto);
                    }
                    enviados++;
                } catch (Exception ex) {
                    // Não marca como visto: tenta de novo na próxima execução.
                    System.out.println("Falha ao enviar aviso de " + filme.titulo() + ": " + ex.getMessage());
                    porChave.keySet().removeAll(chavesNovas);
                }
            }
            porChave.keySet().forEach(k -> estado.marcarVisto(k, hoje));
        }

        estado.limpar(hoje);
        if (!dryRun) estado.salvar(estadoArq);
        System.out.printf("%d aviso(s) %s.%n", enviados, dryRun ? "gerados (dry-run, estado não salvo)" : "enviados");
    }

    /** Uma combinação filme + cinema + versão + formatos. */
    static String chave(Filme f, Sessao s) {
        return f.slug() + "|" + s.cinemaId() + "|" + s.versao() + "|" + String.join("+", s.formatos());
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}

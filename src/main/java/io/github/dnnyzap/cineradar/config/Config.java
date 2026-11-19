package io.github.dnnyzap.cineradar.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dnnyzap.cineradar.model.Sessao.Versao;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Preferências lidas de config.json. Segredos (token do Telegram etc.) vêm de variáveis de ambiente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Config(
        String letterboxdUsuario,
        Integer diasAFrente,
        Set<Versao> versoes,
        Set<String> formatos,
        String horarioMinimo,
        List<Cinema> cinemas,
        String ingressoCidade) {

    /**
     * Um cinema. Pode estar em uma ou nas duas fontes.
     *
     * @param id       id no AdoroCinema (ex.: "F0281"), ou null se o cinema não estiver lá
     * @param nome     nome exibido no aviso
     * @param ingresso id no Ingresso.com (ex.: "1178"), ou null para não consultar o Ingresso.com
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cinema(String id, String nome, String ingresso) {

        public Cinema(String id, String nome) {
            this(id, nome, null);
        }

        /** Identifica o cinema no estado e une as sessões das duas fontes. */
        public String chave() {
            return id != null && !id.isBlank() ? id : "ingresso-" + ingresso;
        }
    }

    public Config {
        diasAFrente = diasAFrente == null ? 7 : diasAFrente;
        versoes = versoes == null || versoes.isEmpty() ? Set.of(Versao.values()) : Set.copyOf(versoes);
        formatos = formatos == null ? Set.of() : Set.copyOf(formatos);
        cinemas = cinemas == null ? List.of() : List.copyOf(cinemas);
    }

    public Config(String letterboxdUsuario, Integer diasAFrente, Set<Versao> versoes, Set<String> formatos,
                  String horarioMinimo, List<Cinema> cinemas) {
        this(letterboxdUsuario, diasAFrente, versoes, formatos, horarioMinimo, cinemas, null);
    }

    public LocalTime horarioMinimoOuNull() {
        return horarioMinimo == null || horarioMinimo.isBlank() ? null : LocalTime.parse(horarioMinimo);
    }

    /** Usuário do Letterboxd: variável de ambiente LETTERBOXD_USUARIO tem prioridade sobre o arquivo. */
    public String usuarioLetterboxd() {
        String env = System.getenv("LETTERBOXD_USUARIO");
        return env != null && !env.isBlank() ? env.trim() : letterboxdUsuario;
    }

    public static Config carregar(Path arquivo) throws IOException {
        return new ObjectMapper().readValue(arquivo.toFile(), Config.class);
    }
}

package io.github.dnnyzap.cineradar.model;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Uma sessão de cinema, vinda do AdoroCinema ou do Ingresso.com.
 *
 * @param id             id da sessão na fonte de origem
 * @param cinemaId       chave do cinema no config (id do AdoroCinema, ex.: "F0281", ou "ingresso-1303")
 * @param cinemaNome     ex.: "Kinoplex Amazonas"
 * @param tituloPtBr     título em português
 * @param tituloOriginal título original
 * @param ano            ano de produção, ou null
 * @param inicio         data e hora local da sessão
 * @param versao         legendado, dublado ou nacional
 * @param formatos       3D, IMAX, VIP etc. (vazio = sala comum 2D)
 * @param linkIngresso   link de compra, ou null
 * @param fonte          de onde a sessão veio
 */
public record Sessao(
        long id,
        String cinemaId,
        String cinemaNome,
        String tituloPtBr,
        String tituloOriginal,
        Integer ano,
        LocalDateTime inicio,
        Versao versao,
        Set<String> formatos,
        String linkIngresso,
        Fonte fonte) {

    public Sessao(long id, String cinemaId, String cinemaNome, String tituloPtBr, String tituloOriginal,
                  Integer ano, LocalDateTime inicio, Versao versao, Set<String> formatos, String linkIngresso) {
        this(id, cinemaId, cinemaNome, tituloPtBr, tituloOriginal, ano, inicio, versao, formatos, linkIngresso,
                Fonte.ADOROCINEMA);
    }

    public enum Fonte { ADOROCINEMA, INGRESSO }

    public enum Versao {
        LEGENDADO, DUBLADO, NACIONAL, DESCONHECIDA;

        /** Converte o "diffusionVersion" do AdoroCinema. */
        public static Versao deAdoroCinema(String valor) {
            if (valor == null) return DESCONHECIDA;
            return switch (valor) {
                case "ORIGINAL" -> LEGENDADO;
                case "DUBBED" -> DUBLADO;
                case "LOCAL" -> NACIONAL;
                default -> DESCONHECIDA;
            };
        }
    }
}

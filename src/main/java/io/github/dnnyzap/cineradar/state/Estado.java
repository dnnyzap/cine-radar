package io.github.dnnyzap.cineradar.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Memória entre execuções, salva em data/estado.json e commitada pelo GitHub Actions.
 *
 * - avisos: chave (filme|cinema|versão|formatos) → último dia em que a combinação foi vista.
 *   Só avisamos quando a combinação aparece pela primeira vez, ou volta depois de sumir por
 *   {@link #DIAS_PARA_REAVISAR} dias. Assim você não recebe a mesma mensagem todo dia.
 * - titulosAlternativos: cache slug → títulos (original, pt-BR) para não consultar o Letterboxd de novo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Estado {

    public static final int DIAS_PARA_REAVISAR = 7;
    private static final int DIAS_PARA_ESQUECER = 30;
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public Map<String, String> avisos = new TreeMap<>();
    public Map<String, List<String>> titulosAlternativos = new TreeMap<>();

    public static Estado carregar(Path arquivo) throws IOException {
        if (!Files.exists(arquivo)) return new Estado();
        Estado e = JSON.readValue(arquivo.toFile(), Estado.class);
        if (e.avisos == null) e.avisos = new TreeMap<>();
        if (e.titulosAlternativos == null) e.titulosAlternativos = new TreeMap<>();
        e.avisos = new TreeMap<>(e.avisos);
        e.titulosAlternativos = new TreeMap<>(e.titulosAlternativos);
        return e;
    }

    public void salvar(Path arquivo) throws IOException {
        if (arquivo.getParent() != null) Files.createDirectories(arquivo.getParent());
        JSON.writeValue(arquivo.toFile(), this);
    }

    /** True se essa combinação merece aviso hoje. */
    public boolean deveAvisar(String chave, LocalDate hoje) {
        String ultima = avisos.get(chave);
        return ultima == null || LocalDate.parse(ultima).isBefore(hoje.minusDays(DIAS_PARA_REAVISAR));
    }

    public void marcarVisto(String chave, LocalDate hoje) {
        avisos.put(chave, hoje.toString());
    }

    public void limpar(LocalDate hoje) {
        avisos.values().removeIf(d -> LocalDate.parse(d).isBefore(hoje.minusDays(DIAS_PARA_ESQUECER)));
    }
}

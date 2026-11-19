package io.github.dnnyzap.cineradar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.net.URISyntaxException;

public final class Fixtures {
    private Fixtures() {}

    public static String ler(String nome) throws IOException {
        try (InputStream in = Fixtures.class.getResourceAsStream("/" + nome)) {
            if (in == null) throw new IOException("fixture não encontrada: " + nome);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static Path caminho(String nome) throws URISyntaxException {
        return Path.of(Fixtures.class.getResource("/" + nome).toURI());
    }
}

package io.github.dnnyzap.cineradar.config;

import io.github.dnnyzap.cineradar.config.Config.Cinema;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigTest {

    @Test
    void leOConfigDoProjeto() throws Exception {
        Config cfg = Config.carregar(Path.of("config.json"));
        assertEquals(14, cfg.diasAFrente());
        assertEquals("35", cfg.ingressoCidade());

        Cinema kinoplex = cfg.cinemas().stream().filter(c -> "F0281".equals(c.id())).findFirst().orElseThrow();
        assertEquals("1178", kinoplex.ingresso());
        assertEquals("F0281", kinoplex.chave());

        Cinema sumauma = cfg.cinemas().stream().filter(c -> "1303".equals(c.ingresso())).findFirst().orElseThrow();
        assertNull(sumauma.id());
        assertEquals("ingresso-1303", sumauma.chave());
    }

    @Test
    void cinemaSemIngressoContinuaValendo() {
        Cinema c = new Cinema("F0281", "Kinoplex");
        assertNull(c.ingresso());
        assertEquals("F0281", c.chave());
    }
}

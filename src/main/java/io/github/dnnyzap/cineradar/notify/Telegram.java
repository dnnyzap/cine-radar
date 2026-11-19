package io.github.dnnyzap.cineradar.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dnnyzap.cineradar.Http;

import java.util.Map;

/** Envia mensagens por um bot do Telegram (https://core.telegram.org/bots/api#sendmessage). */
public class Telegram {

    private final Http http;
    private final String token;
    private final String chatId;
    private final ObjectMapper json = new ObjectMapper();

    public Telegram(Http http, String token, String chatId) {
        this.http = http;
        this.token = token;
        this.chatId = chatId;
    }

    public void enviar(String html) throws Exception {
        String corpo = json.writeValueAsString(Map.of(
                "chat_id", chatId,
                "text", html,
                "parse_mode", "HTML",
                "disable_web_page_preview", true));
        Http.Resposta r = http.postJson("https://api.telegram.org/bot" + token + "/sendMessage", corpo);
        if (!r.ok()) throw new IllegalStateException("Telegram respondeu HTTP " + r.status() + ": " + r.corpo());
    }
}

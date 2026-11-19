# 🎬 Cine Radar

Bot que avisa no Telegram quando um filme da minha watchlist do Letterboxd entra em cartaz nos cinemas de Manaus, só no tipo de sessão que me interessa (legendado, IMAX, à noite...).

Roda sozinho no GitHub Actions a cada 4 horas, sem servidor.

```
Letterboxd ───┐
AdoroCinema ──┼─> cruza filmes ─> filtra preferências ─> Telegram
Ingresso.com ─┘
```

**Stack:** Java 21 · Jsoup · Jackson · JUnit 5 · GitHub Actions

## Destaques

- **Duas fontes de sessões.** AdoroCinema e Ingresso.com, incluindo pré-venda. Quando as duas divergem, vale o Ingresso.com, que é quem vende o ingresso.
- **Cruzamento de títulos.** O Letterboxd usa o título original e os cinemas usam o título em português. A comparação usa títulos normalizados e o ano, com o TMDB opcional como reforço.
- **Sem aviso repetido.** O estado fica versionado em `data/estado.json`, e o próprio workflow faz o commit.
- **Testes com respostas reais** dos sites, salvas em `src/test/resources`.

## Usando

1. Faça um fork e crie um bot no [@BotFather](https://t.me/BotFather).
2. Em **Settings → Secrets and variables → Actions**, cadastre `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID` e a variável `LETTERBOXD_USUARIO`. A watchlist precisa ser pública.
3. Ajuste as preferências no `config.json`:

```json
{
  "diasAFrente": 14,
  "versoes": ["LEGENDADO"],
  "formatos": [],
  "horarioMinimo": null,
  "ingressoCidade": "35",
  "cinemas": [ { "id": "F0281", "ingresso": "1178", "nome": "Kinoplex Amazonas" } ]
}
```

O `config.json` já vem com todos os cinemas de Manaus. `versoes` e `formatos` vazios aceitam qualquer sessão.

Para rodar localmente, sem enviar nada:

```bash
mvn package
LETTERBOXD_USUARIO=seu_usuario java -jar target/cine-radar.jar --dry-run
```

## Limitações

Nenhuma das fontes tem API pública documentada, então uma mudança nos sites pode quebrar o parser. Se o Letterboxd bloquear o Actions (`HTTP 403`), coloque o export da watchlist em `data/watchlist.csv`.

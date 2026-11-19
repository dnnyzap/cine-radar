# 🎬 Cine Radar

Avisa no **Telegram** quando um filme da sua **watchlist do Letterboxd** ganha sessões nos cinemas de **Manaus**, no tipo de sessão que você prefere (ex.: só legendado, só IMAX, só depois das 18h).

Roda de graça no **GitHub Actions** a cada 4 horas. Não precisa de servidor nem VPS.

```
Letterboxd (watchlist) ──┐
AdoroCinema (sessões) ───┼─> cruza títulos + ano ─> filtra preferências ─> Telegram
Ingresso.com (sessões) ──┘                                   │
                                                data/estado.json (não repete aviso)
```

## Como funciona

1. **Watchlist**: lê a página pública `letterboxd.com/<usuario>/watchlist/` (Jsoup). Se não conseguir, usa o `data/watchlist.csv` do export do Letterboxd.
2. **Sessões**: consulta a programação de cada cinema no AdoroCinema e no Ingresso.com (os endpoints JSON usados pelos próprios sites), para os próximos `diasAFrente` dias, incluindo pré-venda. Quando as duas fontes trazem a mesma sessão (mesmo filme, cinema e horário), vale a do Ingresso.com, que é quem vende o ingresso e costuma ter a versão (legendado/dublado) mais correta.
3. **Cruzamento**: o Letterboxd usa o título em inglês e o cinema usa o título em português. O radar compara o título do Letterboxd e o título original (da página do filme) com o título original e o pt-BR da sessão, sem acentos nem pontuação, e confere o ano (±1). Com `TMDB_API_KEY`, também usa o título em pt-BR do TMDB.
4. **Aviso**: uma mensagem por filme, com os horários agrupados por cinema e tipo e o link de compra.
5. **Sem spam**: cada combinação filme + cinema + versão + formato só gera aviso na primeira vez, ou se sumir da programação por mais de 7 dias e voltar.

## Configuração (uns 10 minutos)

### 1. Crie o bot do Telegram
1. No Telegram, fale com **@BotFather**, mande `/newbot` e siga os passos. Guarde o **token**.
2. Mande qualquer mensagem para o seu bot novo.
3. Abra `https://api.telegram.org/bot<SEU_TOKEN>/getUpdates` no navegador e copie o número em `"chat":{"id": ...}`. Esse é o seu **chat id**.

### 2. Suba o projeto para o GitHub
Crie um repositório **público** (no repositório público o Actions é gratuito e ilimitado) e envie este código.

### 3. Configure os segredos
Em **Settings → Secrets and variables → Actions**:

| Tipo | Nome | Valor |
|---|---|---|
| Secret | `TELEGRAM_BOT_TOKEN` | token do BotFather |
| Secret | `TELEGRAM_CHAT_ID` | seu chat id |
| Secret (opcional) | `TMDB_API_KEY` | chave v3 do [TMDB](https://www.themoviedb.org/settings/api); melhora o cruzamento de filmes estrangeiros |
| Variable | `LETTERBOXD_USUARIO` | seu usuário do Letterboxd |

A watchlist precisa estar **pública** no Letterboxd.

### 4. Ajuste o `config.json`

```json
{
  "diasAFrente": 14,
  "versoes": ["LEGENDADO"],
  "formatos": [],
  "horarioMinimo": "18:00",
  "ingressoCidade": "35",
  "cinemas": [ { "id": "F0281", "ingresso": "1178", "nome": "Kinoplex Amazonas" } ]
}
```

- `versoes`: `LEGENDADO`, `DUBLADO`, `NACIONAL`. Vazio = todas.
- `formatos`: vazio = qualquer sala. Exemplos: `["IMAX"]`, `["VIP"]`, `["3D"]`, `["Macro XE"]`, ou `["2D"]` para sala comum (sem 3D/IMAX/Macro XE).
- `horarioMinimo`: ignora sessões antes desse horário. `null` = qualquer horário.
- `diasAFrente`: quantos dias à frente olhar. Com 14, você fica sabendo da pré-venda de estreias com até duas semanas de antecedência.
- `ingressoCidade`: id da cidade no Ingresso.com (Manaus = `35`). `null` desliga o Ingresso.com. Para outra cidade, procure o id em `api-content.ingresso.com/v0/states/<UF>`.
- `cinemas`: o arquivo já vem com todos os cinemas de Manaus. Cada cinema pode ter:
  - `id`: id do AdoroCinema, que aparece na URL da programação (`adorocinema.com/programacao/cinema-F0281/`). Use `null` se o cinema não estiver lá, como o UCI Sumaúma.
  - `ingresso`: id do Ingresso.com. A lista da cidade está em `api-content.ingresso.com/v0/theaters/city/35?partnership=home`. Sem esse campo, o cinema só é consultado no AdoroCinema.

### 5. Teste
Em **Actions → Cine Radar → Run workflow**. O log mostra quantas sessões encontrou em cada cinema e quantos avisos enviou.

## Rodando localmente

```bash
mvn package
java -jar target/cine-radar.jar --dry-run          # mostra as mensagens no terminal, não envia nada
LETTERBOXD_USUARIO=seu_usuario java -jar target/cine-radar.jar --dry-run
```

Sem `TELEGRAM_BOT_TOKEN` e `TELEGRAM_CHAT_ID`, o programa entra em `--dry-run` sozinho.

## Se o Letterboxd bloquear o GitHub Actions

Sites atrás de Cloudflare às vezes bloqueiam IPs de datacenter. Se o log mostrar `HTTP 403` na watchlist:
1. No Letterboxd, vá em **Settings → Data → Export your data**.
2. Copie o `watchlist.csv` do zip para `data/watchlist.csv` e faça commit.

O radar usa esse arquivo como reserva automaticamente. A desvantagem é que você precisa reexportar quando adicionar filmes.

## Estrutura

```
src/main/java/io/github/dnnyzap/cineradar/
├── CineRadar.java            # orquestra uma execução
├── Http.java                 # HttpClient com User-Agent, pausa entre requisições e retry
├── config/Config.java        # config.json + variáveis de ambiente
├── letterboxd/               # watchlist (HTML e CSV) + títulos alternativos
├── adorocinema/              # programação dos cinemas no AdoroCinema (JSON)
├── ingresso/                 # programação dos cinemas no Ingresso.com (JSON)
├── matching/Matcher.java     # cruza títulos/ano e aplica preferências
├── notify/                   # texto da mensagem + envio pelo Telegram
└── state/Estado.java         # o que já foi avisado (data/estado.json)
```

## Limitações conhecidas

- AdoroCinema, Ingresso.com e Letterboxd não têm API pública documentada para isso: se o site mudar, o parser precisa de ajuste. Os testes usam respostas reais salvas em `src/test/resources`, então fica fácil ver o que quebrou.
- Filmes estrangeiros com título muito diferente podem não casar sem a `TMDB_API_KEY`.
- O GitHub desativa workflows agendados de repositórios públicos sem atividade por 60 dias. Se isso acontecer, é só reativar na aba Actions.

## Ideias para a v2

- Interface web (Spring Boot + React) para várias pessoas cadastrarem o usuário do Letterboxd e as preferências.
- Comando `/agora` no bot para ver as sessões do dia.
- Aviso de pré-estreia e de "última semana em cartaz".

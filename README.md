# Brain Cache

> Spaced-repetition knowledge forge with a daily email nudge — built on Spring Boot + gRPC.

Brain Cache is a personal memory engine. You capture the things you want to remember
as **cards** (a prompt on the front, the answer on the back); the app resurfaces them
on a widening schedule so a fact is reviewed right before you'd forget it. It also
recommends a most-asked **DSA problem** each day — solve it, mark it done, and it joins
the same review rotation.

Every day it sends two emails: **Revision** (cards due today) and **Recommendation**
(today's DSA problem to solve).

It doubles as a from-scratch Spring Boot learning project — the source is heavily
commented with Go parallels for anyone coming from a gRPC/Go background.

---

## Features

- **JWT auth over gRPC** — register / login issue a signed token; a global server
  interceptor verifies it and binds the caller's identity into the gRPC context.
- **Spaced repetition** — a config-driven interval ladder (`1, 2, 4, 8, 16, 32, 64, 128`
  days). A passed review climbs one rung; a failed one drops back to the first.
- **Daily Revision email** — a cron job groups every due card by user and emails each
  person their review list.
- **Daily DSA Recommendation email** — a per-difficulty weekly schedule picks the
  most-asked unseen LeetCode problems (dataset in `data/dsa/`). Marking one done turns
  it into a card that flows into the Revision rotation.
- **Adminer DB panel** — a browser UI to view/edit Postgres, bound to localhost only.
- **Env-var driven config** — every secret and tunable binds from environment variables,
  ready for containerised deploys.

## Architecture

```
gRPC client ──▶ JwtServerInterceptor ──▶ *GrpcService ──▶ *Service ──▶ Repository ──▶ Postgres
                (verifies JWT,             (proto <-> domain)  (business    (Spring Data
                 sets identity in ctx)                          logic)       derived queries)

  DailyReviewJob   (cron) ─▶ EmailService ─▶ SMTP    "Brain-Cache: Revision"
  RecommendationJob(cron) ─▶ EmailService ─▶ SMTP    "Brain-Cache: Recommendation"
```

Layers are kept strictly separated: the gRPC classes know only about proto types and
error mapping; the service classes hold the business logic and never touch proto; the
repositories are declarative interfaces whose implementations Spring generates.

## Tech stack

| Concern        | Choice                                             |
| -------------- | -------------------------------------------------- |
| Language       | Java 17                                            |
| Framework      | Spring Boot 3.2                                     |
| Transport      | gRPC (`net.devh` server starter) on `:9090`        |
| Persistence    | Spring Data JPA + PostgreSQL                        |
| Auth           | Spring Security (bcrypt) + JWT (`jjwt`)             |
| Email          | Spring Mail (SMTP)                                  |
| DB admin       | Adminer (Docker, localhost-only)                   |
| Build          | Maven (protobuf compiled during build)             |

## Project layout

```
proto/brain_cache.proto              # gRPC contract (source of truth)
data/dsa/{hard,medium,easy}.csv      # one-time DSA dataset, sorted by #companies
src/main/java/com/braincache/
├── config/       # typed @ConfigurationProperties + bean wiring
├── domain/       # JPA entities (User, Card, CardType)
├── repository/   # Spring Data interfaces
├── security/     # JWT service + gRPC auth interceptor
├── service/      # business logic (Auth, Card, Email, DsaCatalog, DsaRecommendation)
├── scheduler/    # DailyReviewJob + RecommendationJob crons
└── grpc/         # gRPC transport adapters
```

## Getting started

### Prerequisites

- JDK 17+
- Maven 3.9+
- Docker (for local Postgres + Adminer)

### 1. Configure

```bash
cp .env.example .env
```

Fill in `.env`:

- `JWT_SECRET` — generate with `openssl rand -base64 48` (min 32 bytes).
- `SMTP_USERNAME` / `SMTP_PASSWORD` — for Gmail, create an **app password**
  (not your account password). Leave blank to run without email.

### 2. Start Postgres (+ Adminer)

```bash
docker compose up -d
```

Adminer UI: <http://localhost:8080> — System `PostgreSQL`, Server `postgres`, then
your `.env` credentials. It's bound to `127.0.0.1` only; on a server reach it via an
SSH tunnel (`ssh -L 8080:localhost:8080 …`), never by opening the port publicly.

### 3. Run the backend

```bash
set -a && source .env && set +a   # export vars into the shell
mvn spring-boot:run
```

The gRPC server comes up on `localhost:9090`.

### Try it

Using [`grpcurl`](https://github.com/fullstorydev/grpcurl) (server reflection is enabled by `net.devh`):

```bash
# Register — returns a JWT
grpcurl -plaintext -d '{"email":"me@example.com","password":"hunter2"}' \
  localhost:9090 braincache.v1.AuthService/Register

# Create a card (send the token in metadata)
grpcurl -plaintext -H "authorization: Bearer <TOKEN>" \
  -d '{"front":"Reverse a linked list?","back":"Three pointers: prev, curr, next."}' \
  localhost:9090 braincache.v1.CardService/CreateCard

# List cards due now
grpcurl -plaintext -H "authorization: Bearer <TOKEN>" \
  -d '{}' localhost:9090 braincache.v1.CardService/ListDueCards

# Record a review outcome (advances / resets the interval)
grpcurl -plaintext -H "authorization: Bearer <TOKEN>" \
  -d '{"cardId":1,"passed":true}' localhost:9090 braincache.v1.CardService/ReviewCard

# Mark a recommended DSA problem done -> it joins the revision rotation
grpcurl -plaintext -H "authorization: Bearer <TOKEN>" \
  -d '{"url":"https://leetcode.com/problems/trapping-rain-water","comment":"two pointers"}' \
  localhost:9090 braincache.v1.CardService/MarkDsaProblemDone
```

## Configuration reference

All values are environment variables with sensible local defaults.

| Variable                     | Default                          | Purpose                                     |
| ---------------------------- | -------------------------------- | ------------------------------------------- |
| `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://…/braincache` | Postgres JDBC URL                           |
| `SPRING_DATASOURCE_USERNAME` | `braincache`                     | DB user                                     |
| `SPRING_DATASOURCE_PASSWORD` | `braincache`                     | DB password                                 |
| `JWT_SECRET`                 | dev placeholder                  | HMAC signing key (**set in prod**)          |
| `JWT_TTL_HOURS`              | `168`                            | Token lifetime (7 days)                     |
| `SMTP_HOST` / `SMTP_PORT`    | `smtp.gmail.com` / `587`         | SMTP server                                 |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | –                           | SMTP credentials                            |
| `REVIEW_INTERVALS_DAYS`      | `1,2,4,8,16,32,64,128`           | Spaced-repetition interval ladder           |
| `REVIEW_EMAIL_CRON`          | `0 0 7 * * *`                    | Revision email schedule (Spring cron)       |
| `REVIEW_EMAIL_FROM`          | `brain-cache@localhost`          | Email sender address                        |
| `RECOMMENDATION_CRON`        | `0 0 8 * * *`                    | Recommendation email schedule               |
| `RECOMMENDATION_DATA_DIR`    | `data/dsa`                       | Folder holding the DSA CSVs                 |
| `RECOMMENDATION_HARD`        | `1,1,1,1,1,1,1`                  | Hard problems per weekday (Mon..Sun)        |
| `RECOMMENDATION_MEDIUM`      | `0,0,0,0,0,0,0`                  | Medium problems per weekday                 |
| `RECOMMENDATION_EASY`        | `0,0,0,0,0,0,0`                  | Easy problems per weekday                   |

## DSA dataset

`data/dsa/{hard,medium,easy}.csv` is a one-time static export (snapshot May 2026) of
company-wise LeetCode questions, sourced from a public repository that scraped LeetCode
Premium's company question lists. Problems are deduplicated and sorted by how many
companies ask each. The recommender walks each list top-down, skipping problems already
done.

## Roadmap

- [x] Daily DSA recommendation email
- [ ] Resolve reviews directly from the email (tokenised pass/fail links)
- [ ] Flyway migrations (replace Hibernate `ddl-auto`)
- [ ] React gRPC-Web frontend (Envoy + nginx) for EC2
- [ ] CI/CD + SSL

## License

Released under the [MIT License](LICENSE).

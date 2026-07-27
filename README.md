# Brain Cache

> Spaced-repetition knowledge forge with a daily email nudge — built on Spring Boot + gRPC.

Brain Cache is a personal memory engine. You capture the things you want to remember
as **cards** (a prompt on the front, the answer on the back); the app resurfaces them
on a widening schedule so a fact is reviewed right before you'd forget it. Every
morning it emails you the cards that are due.

It doubles as a from-scratch Spring Boot learning project — the source is heavily
commented with Go parallels for anyone coming from a gRPC/Go background.

---

## Features

- **JWT auth over gRPC** — register / login issue a signed token; a global server
  interceptor verifies it and binds the caller's identity into the gRPC context.
- **Spaced repetition** — a config-driven interval ladder (`1, 2, 4, 8, 16, 32, 64, 128`
  days). A passed review climbs one rung; a failed one drops back to the first.
- **Daily review email** — a cron job groups every due card by user and emails each
  person their review list.
- **Env-var driven config** — every secret and tunable binds from environment variables,
  ready for containerised deploys.

## Architecture

```
gRPC client ──▶ JwtServerInterceptor ──▶ *GrpcService ──▶ *Service ──▶ Repository ──▶ Postgres
                (verifies JWT,             (proto <-> domain)  (business    (Spring Data
                 sets identity in ctx)                          logic)       derived queries)

                                      DailyReviewJob (cron) ──▶ EmailService ──▶ SMTP
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
| Build          | Maven (protobuf compiled during build)             |

## Project layout

```
proto/brain_cache.proto              # gRPC contract (source of truth)
src/main/java/com/braincache/
├── config/       # typed @ConfigurationProperties + bean wiring
├── domain/       # JPA entities (User, Card)
├── repository/   # Spring Data interfaces
├── security/     # JWT service + gRPC auth interceptor
├── service/      # business logic (Auth, Card, Email)
├── scheduler/    # DailyReviewJob cron
└── grpc/         # gRPC transport adapters
```

## Getting started

### Prerequisites

- JDK 17+
- Maven 3.9+
- Docker (for the local Postgres)

### 1. Configure

```bash
cp .env.example .env
```

Fill in `.env`:

- `JWT_SECRET` — generate with `openssl rand -base64 48` (min 32 bytes).
- `SMTP_USERNAME` / `SMTP_PASSWORD` — for Gmail, create an **app password**
  (not your account password). Leave blank to run without email.

### 2. Start Postgres

```bash
docker compose up -d postgres
```

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
```

## Configuration reference

All values are environment variables with sensible local defaults.

| Variable                 | Default                          | Purpose                                   |
| ------------------------ | -------------------------------- | ----------------------------------------- |
| `SPRING_DATASOURCE_URL`  | `jdbc:postgresql://…/braincache` | Postgres JDBC URL                         |
| `SPRING_DATASOURCE_USERNAME` | `braincache`                 | DB user                                   |
| `SPRING_DATASOURCE_PASSWORD` | `braincache`                 | DB password                               |
| `JWT_SECRET`             | dev placeholder                  | HMAC signing key (**set in prod**)        |
| `JWT_TTL_HOURS`          | `168`                            | Token lifetime (7 days)                   |
| `SMTP_HOST` / `SMTP_PORT`| `smtp.gmail.com` / `587`         | SMTP server                               |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | –                       | SMTP credentials                          |
| `REVIEW_INTERVALS_DAYS`  | `1,2,4,8,16,32,64,128`           | Spaced-repetition interval ladder         |
| `REVIEW_EMAIL_CRON`      | `0 0 7 * * *`                    | Daily email schedule (Spring cron)        |
| `REVIEW_EMAIL_FROM`      | `brain-cache@localhost`          | Review email sender address               |

## Roadmap

- [ ] Resolve reviews directly from the email (tokenised pass/fail links)
- [ ] Daily DSA problem email
- [ ] Flyway migrations (replace Hibernate `ddl-auto`)
- [ ] Deploy stack (Envoy + nginx) for EC2

## License

Personal project — all rights reserved.

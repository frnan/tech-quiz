# Tech Quiz

Train for technical interviews with single-choice questions, each followed by an explanation
and references. The first question set covers a Java/Spring Boot + Angular stack: 15 topics,
150 questions, a third of them about a code snippet.

- **Backend**: Java, Spring Boot, Spring Security & JWT, JPA & Hibernate, SQL/PostgreSQL & Flyway,
  Redis & caching, REST & OpenAPI, testing (JUnit, Mockito, Testcontainers)
- **Frontend**: TypeScript, Angular, RxJS & signals
- **Delivery and operations**: Docker, Git & CI/CD, observability, web security

## Run it

All you need is Docker.

```bash
git clone <this repo> && cd tech-quiz
docker compose up --build
```

Open http://localhost:8080. The first build takes a few minutes (Maven and npm downloads).
After that, `docker compose up` starts in seconds.

To use another port: `TECH_QUIZ_PORT=9090 docker compose up --build`.

## How it works

- **Quick mix**: 10, 20 or 30 random questions from every topic.
- **Custom quiz**: pick topics and a size. Questions are spread evenly across the topics.
- Each answer is checked by the server, which then shows the explanation and the references.
- **Results**: score per topic, every mistake explained, and a *Retry my mistakes* quiz.
- **History**: past sessions and "topics to work on" are kept in your browser (localStorage).
  There are no accounts and nothing personal is stored on the server.

```
questions/*.yaml ──(startup sync)──▶ MongoDB ──▶ Spring Boot /api/v1 ──▶ Angular SPA
   source of truth                  read model     same origin, one image
```

| Endpoint | |
|---|---|
| `GET /api/v1/topics` | Topics with their question counts |
| `POST /api/v1/quizzes` | `{size}` quick mix · `{topicIds, size}` custom · `{questionIds}` retry. Options come without the answer. |
| `POST /api/v1/questions/{id}/answer` | `{optionId}` → `{correct, correctOptionId, explanation, references}` |

Swagger UI is at http://localhost:8080/swagger-ui.html.

Design decisions are recorded in [`docs/adr/`](docs/adr/):
[stack](docs/adr/0001-stack.md),
[YAML files synced into MongoDB](docs/adr/0002-yaml-source-of-truth-synced-to-mongo.md),
[one image](docs/adr/0003-single-image-monorepo.md),
[stateless server-side scoring](docs/adr/0004-stateless-server-side-scoring.md).

## Edit or add questions

Questions are plain YAML, one file per topic in [`questions/`](questions/):

```yaml
- id: docker-011
  prompt: What does `docker compose down -v` remove?
  options:
    - text: The containers, the networks and the named volumes
      correct: true
    - text: Only the containers
    - text: The images
  explanation: >-
    `-v` also removes the named volumes declared in the compose file, so the data is lost.
  references:
    - https://docs.docker.com/reference/cli/docker/compose/down/
```

With compose running, save the file and run `docker compose restart app`. An invalid file stops
startup with a message naming the file and the problem. See [CONTRIBUTING.md](CONTRIBUTING.md)
for the rules and for how to add a topic.

## Develop

Requirements: Java 21, Node 24, Docker (for MongoDB and the Testcontainers tests).

```bash
docker run -d --name tech-quiz-mongo -p 27017:27017 mongo:8.0   # or any MongoDB on :27017
cd backend && ./mvnw spring-boot:run                             # API on :8080, reads ../questions
cd frontend && npm ci && npm start                               # SPA on :4200, /api proxied to :8080
```

`MONGODB_URI` and `QUIZ_QUESTIONS_PATH` override the defaults (`mongodb://localhost:27017/techquiz`, `../questions`).

| | Command | Gate |
|---|---|---|
| Backend tests | `cd backend && ./mvnw verify` | JaCoCo: ≥80% lines, ≥75% branches, ≥80% branches per service |
| Frontend tests | `cd frontend && npm run test:ci` | Vitest: ≥80% statements/lines/functions, ≥75% branches |
| Lint / format | `npm run lint`, `npm run format:check` | zero warnings |

CI (GitHub Actions) runs all of the above on every PR, then builds the image. On `main` it
also pushes the image to `ghcr.io/<owner>/tech-quiz`.

## License

[MIT](LICENSE)

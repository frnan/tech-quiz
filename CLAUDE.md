# CLAUDE.md — Tech Quiz

Interview-training quiz. Monorepo: `backend/` (Java 21, Spring Boot 4.1, MongoDB), `frontend/`
(Angular 22 zoneless, signals, Vitest), `questions/` (YAML, the source of truth). Born as a
companion to HomeQuest (sibling directories `homequest-backend`/`homequest-frontend`), whose stack
the first question set covers. The maintainer uses it to check they understand that stack, so
explain the "why" in PRs.

## Decisions (docs/adr/)

- 0001 Spring Boot + Angular + MongoDB.
- 0002 YAML files → MongoDB sync at startup (`QuestionSyncService`): upsert by id, delete stale,
  **fail fast** on an invalid file. Mongo is a disposable read model.
- 0003 One image: the Angular build is copied into Spring Boot's `static/`; `SpaWebConfig` sends
  non-file, non-API paths to `index.html`.
- 0004 Stateless: quizzes never expose `correct`; `POST /questions/{id}/answer` checks one answer.
  History lives in the browser's localStorage only.

## Conventions

- Backend: package by feature (`question`, `quiz`, `web`); records for documents and DTOs;
  constructor injection; errors are `ProblemDetail` (`ApiExceptionHandler`). Randomness comes
  from an injected `RandomGenerator` so tests can seed it.
- Frontend: same conventions as homequest-frontend. Standalone components with separate
  `.ts/.html/.scss/.spec.ts` files, `@Service()` signal services where only the owning service
  writes its signals, design tokens in `src/styles/_tokens.scss`, 44px touch targets. No
  `innerHTML` except highlight.js output (`CodeBlock`), which escapes its input. Question text
  goes through `RichText`, which renders text nodes only.
- Code languages are listed in three places that must agree: `questions/schema.json`,
  `QuestionFilesContentTest`, `frontend/src/app/shared/code-block/highlight.ts`.
- Coverage gates: backend JaCoCo ≥80% lines / ≥75% branches (≥80% branches per `*Service`);
  frontend ≥80% statements/lines/functions, ≥75% branches.
- Trunk-based: `main` + short-lived `feature/*` branches; Conventional Commits.

## Commands

- `docker compose up --build`: full app on :8080.
- `cd backend && ./mvnw verify`: tests (Testcontainers needs Docker) + coverage + question files.
- `cd frontend && npm run lint && npm run test:ci && npm run build`.

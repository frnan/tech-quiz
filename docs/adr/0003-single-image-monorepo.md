# 0003: Monorepo, one image that serves both the API and the SPA

- Status: Accepted
- Date: 2026-10-25

## Context
HomeQuest is two repositories, with an nginx in front. For a public project that people clone to
try out, the priority is "one command and it runs".

## Decision
- One repository: `backend/`, `frontend/`, `questions/`, and one CI workflow.
- A multi-stage `Dockerfile` builds the Angular app and copies it into Spring Boot's
  `static/`. Spring Boot then serves the SPA and the `/api/v1` endpoints from a single origin
  (no CORS). A forward controller sends unknown non-API paths to `index.html`.
- `docker compose up` starts that image plus MongoDB. `./questions` is mounted read-only.

## Consequences
- Frontend and backend are always deployed together, so they can't drift apart in version.
- In dev, `ng serve` proxies `/api` to `localhost:8080`.
- A frontend-only change rebuilds the whole image. That's fine at this size.

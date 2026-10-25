# 0001: Spring Boot + Angular + MongoDB

- Status: Accepted
- Date: 2026-10-25

## Context
Tech Quiz trains people for interviews on the HomeQuest stack (Java/Spring Boot, Angular, Docker, CI...).
Its maintainer also wants to practise that same stack while building it. The questions must be
easy to edit as files, and the app needs a NoSQL store.

## Decision
- Backend: Java 21, Spring Boot 4 (Web MVC, Data MongoDB, Validation, Actuator).
- Frontend: Angular 22 (standalone, zoneless, signals), tested with Vitest.
- Database: MongoDB 8. A question is a self-contained document (prompt, code, options,
  explanation), so a document store fits it without joins or migrations.

## Consequences
- The quiz content and the code exercise the same technologies.
- The image is heavier than a Node or static alternative (a JRE ships inside it).
- With no relational schema there's no Flyway. The YAML files are the schema's documentation (ADR 0002).

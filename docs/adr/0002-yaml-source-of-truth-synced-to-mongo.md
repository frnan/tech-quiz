# 0002: YAML files are the source of truth, synced into MongoDB at startup

- Status: Accepted
- Date: 2026-10-25

## Context
Contributors should be able to add or fix a question with a pull request, without running a database
tool. The app still needs fast querying: filter by topic, random sampling, counts per topic.

## Decision
- One YAML file per topic in `questions/`. Its format is documented in `questions/schema.json`.
- On `ApplicationReadyEvent`, `QuestionSyncService` loads every file, validates it, upserts each topic
  and question by id, and deletes the ones that are no longer in the files.
- An invalid file **stops startup** with a message naming the file and the field.
- A unit test loads the real `questions/` directory, so CI rejects a broken contribution before
  it's merged.

## Consequences
- Mongo is a disposable read model. It can be dropped at any time, and the next start rebuilds it.
- Editing a question at runtime takes a restart (`docker compose restart app`). That's acceptable
  for content that changes through pull requests.
- No admin UI and no write API, so there's less attack surface on a public app.

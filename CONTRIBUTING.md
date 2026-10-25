# Contributing

Contributions are welcome, especially new questions and fixes to existing ones.

## Adding or fixing a question

1. Open the topic's file in [`questions/`](questions/), e.g. `questions/angular.yaml`.
2. Add a question at the end with the next free id (`angular-011`). **Never reuse or renumber
   an id**: ids are how the database and people's history refer to a question.
3. Check it: `cd backend && ./mvnw test -Dtest=QuestionFilesContentTest` (or run the full app).
4. Open a pull request.

Editors that understand `# yaml-language-server: $schema=./schema.json` (VS Code with the YAML
extension, IntelliJ) validate the file as you type.

### Format

| Field | Required | Notes |
|---|---|---|
| `id` | yes | `<topic-id>-NNN`, unique |
| `prompt` | yes | The question. `` `inline code` `` is rendered as code |
| `code` | no | `{ language, source }`, shown under the prompt. Languages: java, typescript, javascript, html, sql, yaml, dockerfile, bash, json, xml, plaintext |
| `options` | yes | 3 to 5 `{ text }` entries, **exactly one** with `correct: true`. They're shuffled when shown |
| `explanation` | yes | Shown after answering |
| `references` | no | http(s) links to official docs, RFCs, or well-known articles |

### What makes a good question

- **It tests understanding, not trivia.** "Why does `@Transactional` not apply here?" beats
  "Which year was Spring released?". Interviewers ask about trade-offs, pitfalls and behaviour.
- **One answer is unambiguously right** today. Name the version when behaviour changed between
  versions (e.g. "Java 21", "Angular 19+").
- **Wrong options are plausible.** Each one should be a mistake someone could really make.
  Avoid "all of the above" and joke answers.
- **The explanation says why the answer is right *and* why the trap is tempting.** Two to four
  sentences.
- **Keep code snippets short**: 15 lines at most, only what the question needs.
- Write in English, and quote YAML strings that contain `: `, `#`, or start with `@`, `` ` `` or `{`.

## Adding a topic

1. Create `questions/<topic-id>.yaml` (kebab-case). The file name must match `topic.id`:

   ```yaml
   # yaml-language-server: $schema=./schema.json
   topic:
     id: kubernetes
     name: Kubernetes
     description: Pods, deployments, services, probes and config.
   questions:
     - id: kubernetes-001
       ...
   ```

2. Add **at least 5 questions** (the content test enforces it).

No code change is needed: topics are discovered from the files.

## Code changes

- Backend: Java 21 / Spring Boot 4 in `backend/`. Run `./mvnw verify`.
- Frontend: Angular 22 in `frontend/`. Run `npm run lint && npm run test:ci`.
- Both have coverage gates in CI. Add tests along with the change.
- Architecturally significant decisions get a short ADR in `docs/adr/` (context, decision,
  consequences).
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/)
  (`feat:`, `fix:`, `docs:`...).

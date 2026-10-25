# 0004: Stateless quizzes, answers checked server-side, history in the browser

- Status: Accepted
- Date: 2026-10-25

## Context
The app has no accounts. Yet a quiz client that receives the correct answers up front can be
"solved" from the devtools network tab, which defeats the point of interview training.

## Decision
- `POST /api/v1/quizzes` returns questions whose options carry an opaque id and text, never
  the `correct` flag. Options are shuffled per quiz.
- `POST /api/v1/questions/{id}/answer` checks one answer and returns the correct option id, the
  explanation and the references.
- The server keeps **no quiz session**: each answer is checked on its own. The client holds the
  session state, and keeps a history of past sessions in `localStorage`.

## Consequences
- No user data is stored server-side, so there's nothing to secure or to delete for GDPR.
- Someone determined can still read the answers in the public YAML files. The aim is to
  avoid an accidental spoiler, not to be cheat-proof.
- History doesn't follow the user across browsers. Accounts would be a later ADR.

import { Service, computed, inject, signal } from '@angular/core';

import { HistoryService, TopicScore } from './history.service';
import { AnswerResult, QuizMode, QuizQuestion, QuizRequest } from './models';
import { QuizApiService } from './quiz-api.service';

export interface Answer {
  readonly optionId: string;
  readonly result: AnswerResult;
}

export type SessionStatus = 'idle' | 'loading' | 'playing' | 'finished' | 'error';

/** A wrong answer, with what the results page needs to explain it. */
export interface Mistake {
  readonly question: QuizQuestion;
  readonly chosenText: string;
  readonly correctText: string;
  readonly result: AnswerResult;
}

/**
 * The quiz being played. Only this service writes its signals. Answers are checked by the
 * backend one at a time (ADR 0004); the finished session goes to the history.
 */
@Service()
export class QuizSessionService {
  private readonly api = inject(QuizApiService);
  private readonly history = inject(HistoryService);

  private readonly state = signal<SessionStatus>('idle');
  private readonly mode = signal<QuizMode>('quick');
  private readonly list = signal<readonly QuizQuestion[]>([]);
  private readonly position = signal(0);
  private readonly given = signal<ReadonlyMap<string, Answer>>(new Map());
  private readonly submitting = signal(false);
  private readonly error = signal<string | null>(null);

  readonly status = this.state.asReadonly();
  readonly quizMode = this.mode.asReadonly();
  readonly questions = this.list.asReadonly();
  readonly index = this.position.asReadonly();
  readonly answers = this.given.asReadonly();
  readonly isSubmitting = this.submitting.asReadonly();
  readonly errorMessage = this.error.asReadonly();

  readonly current = computed<QuizQuestion | null>(() => this.list()[this.position()] ?? null);
  readonly currentAnswer = computed(() => {
    const question = this.current();
    return question ? (this.given().get(question.id) ?? null) : null;
  });
  readonly isLast = computed(() => this.position() === this.list().length - 1);
  readonly correctCount = computed(
    () => [...this.given().values()].filter((answer) => answer.result.correct).length,
  );
  readonly scorePercent = computed(() =>
    this.list().length ? Math.round((100 * this.correctCount()) / this.list().length) : 0,
  );

  readonly topicScores = computed(() => {
    const scores = new Map<string, TopicScore>();
    for (const question of this.list()) {
      const answer = this.given().get(question.id);
      if (!answer) {
        continue;
      }
      const score = scores.get(question.topicId) ?? { answered: 0, correct: 0 };
      scores.set(question.topicId, {
        answered: score.answered + 1,
        correct: score.correct + (answer.result.correct ? 1 : 0),
      });
    }
    return scores;
  });

  readonly mistakes = computed<Mistake[]>(() =>
    this.list().flatMap((question) => {
      const answer = this.given().get(question.id);
      if (!answer || answer.result.correct) {
        return [];
      }
      const textOf = (id: string) =>
        question.options.find((option) => option.id === id)?.text ?? '';
      return [
        {
          question,
          chosenText: textOf(answer.optionId),
          correctText: textOf(answer.result.correctOptionId),
          result: answer.result,
        },
      ];
    }),
  );

  /** Fetches a new quiz and starts it. Resolves once it's playable (or failed). */
  async start(request: QuizRequest, mode: QuizMode): Promise<void> {
    this.state.set('loading');
    this.mode.set(mode);
    this.error.set(null);
    try {
      const quiz = await this.api.createQuiz(request);
      if (quiz.questions.length === 0) {
        this.fail('No question matches this selection.');
        return;
      }
      this.list.set(quiz.questions);
      this.position.set(0);
      this.given.set(new Map());
      this.state.set('playing');
    } catch {
      this.fail('Could not load the quiz. Is the backend running?');
    }
  }

  /** A new quiz with exactly the questions answered wrong. */
  retryMistakes(): Promise<void> {
    const questionIds = this.mistakes().map((mistake) => mistake.question.id);
    return this.start({ questionIds }, 'retry');
  }

  /** Sends the answer to the current question. Ignored if it's already answered or in flight. */
  async answer(optionId: string): Promise<void> {
    const question = this.current();
    if (!question || this.currentAnswer() || this.submitting() || this.state() !== 'playing') {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    try {
      const result = await this.api.answer(question.id, optionId);
      this.given.update((answers) => new Map(answers).set(question.id, { optionId, result }));
    } catch {
      this.error.set('Could not check the answer. Try again.');
    } finally {
      this.submitting.set(false);
    }
  }

  /** Moves to the next question, or finishes the quiz after the last one. */
  next(): void {
    if (this.state() !== 'playing' || !this.currentAnswer()) {
      return;
    }
    if (this.isLast()) {
      this.finish();
    } else {
      this.position.update((index) => index + 1);
    }
  }

  private finish(): void {
    this.state.set('finished');
    this.history.record({
      finishedAt: new Date().toISOString(),
      mode: this.mode(),
      total: this.list().length,
      correct: this.correctCount(),
      topics: Object.fromEntries(this.topicScores()),
    });
  }

  private fail(message: string): void {
    this.error.set(message);
    this.state.set('error');
  }
}

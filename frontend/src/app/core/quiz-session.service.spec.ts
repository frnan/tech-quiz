import { TestBed } from '@angular/core/testing';

import { HistoryService } from './history.service';
import { QuizApiService } from './quiz-api.service';
import { QuizSessionService } from './quiz-session.service';
import { ApiMock, apiMock, question } from '../../testing/fixtures';

describe('QuizSessionService', () => {
  let api: ApiMock;
  let session: QuizSessionService;
  let history: HistoryService;

  beforeEach(() => {
    localStorage.clear();
    api = apiMock();
    TestBed.configureTestingModule({ providers: [{ provide: QuizApiService, useValue: api }] });
    session = TestBed.inject(QuizSessionService);
    history = TestBed.inject(HistoryService);
  });

  async function startWith(...ids: [string, string][]) {
    api.createQuiz.mockResolvedValue({ questions: ids.map(([id, topic]) => question(id, topic)) });
    await session.start({ size: ids.length }, 'quick');
  }

  it('starts idle with nothing to show', () => {
    expect(session.status()).toBe('idle');
    expect(session.current()).toBeNull();
    expect(session.currentAnswer()).toBeNull();
    expect(session.scorePercent()).toBe(0);
  });

  it('loads a quiz and starts on the first question', async () => {
    let resolve!: (quiz: { questions: ReturnType<typeof question>[] }) => void;
    api.createQuiz.mockReturnValue(new Promise((r) => (resolve = r)));

    const started = session.start({ topicIds: ['java'], size: 2 }, 'custom');
    expect(session.status()).toBe('loading');
    resolve({ questions: [question('java-001'), question('java-002')] });
    await started;

    expect(api.createQuiz).toHaveBeenCalledWith({ topicIds: ['java'], size: 2 });
    expect(session.status()).toBe('playing');
    expect(session.quizMode()).toBe('custom');
    expect(session.current()?.id).toBe('java-001');
    expect(session.isLast()).toBe(false);
  });

  it('fails when the backend returns no question', async () => {
    api.createQuiz.mockResolvedValue({ questions: [] });
    await session.start({}, 'quick');
    expect(session.status()).toBe('error');
    expect(session.errorMessage()).toContain('No question');
  });

  it('fails when the backend is unreachable', async () => {
    api.createQuiz.mockRejectedValue(new Error('offline'));
    await session.start({}, 'quick');
    expect(session.status()).toBe('error');
    expect(session.errorMessage()).toContain('Could not load');
  });

  it('checks an answer once and keeps the result', async () => {
    await startWith(['java-001', 'java']);

    await session.answer('java-001-b');
    await session.answer('java-001-a');

    expect(api.answer).toHaveBeenCalledTimes(1);
    expect(session.currentAnswer()).toEqual({
      optionId: 'java-001-b',
      result: expect.objectContaining({ correct: false, correctOptionId: 'java-001-a' }),
    });
  });

  it('ignores a second click while the first answer is in flight', async () => {
    await startWith(['java-001', 'java']);
    const first = session.answer('java-001-a');
    expect(session.isSubmitting()).toBe(true);
    await session.answer('java-001-b');
    await first;

    expect(api.answer).toHaveBeenCalledTimes(1);
    expect(session.isSubmitting()).toBe(false);
  });

  it('ignores answers when no quiz is playing', async () => {
    await session.answer('x');
    expect(api.answer).not.toHaveBeenCalled();
  });

  it('reports an answer that could not be checked, and allows another try', async () => {
    await startWith(['java-001', 'java']);
    api.answer.mockRejectedValueOnce(new Error('offline'));

    await session.answer('java-001-a');
    expect(session.errorMessage()).toContain('Could not check');
    expect(session.currentAnswer()).toBeNull();

    await session.answer('java-001-a');
    expect(session.currentAnswer()?.result.correct).toBe(true);
    expect(session.errorMessage()).toBeNull();
  });

  it('only moves on once the current question is answered', async () => {
    await startWith(['java-001', 'java'], ['docker-001', 'docker']);
    session.next();
    expect(session.index()).toBe(0);

    await session.answer('java-001-a');
    session.next();
    expect(session.index()).toBe(1);
    expect(session.isLast()).toBe(true);
  });

  it('finishes after the last question, scores it and records it in the history', async () => {
    await startWith(['java-001', 'java'], ['java-002', 'java'], ['docker-001', 'docker']);
    await session.answer('java-001-a');
    session.next();
    await session.answer('java-002-c');
    session.next();
    await session.answer('docker-001-a');
    session.next();

    expect(session.status()).toBe('finished');
    expect(session.correctCount()).toBe(2);
    expect(session.scorePercent()).toBe(67);
    expect(Object.fromEntries(session.topicScores())).toEqual({
      java: { answered: 2, correct: 1 },
      docker: { answered: 1, correct: 1 },
    });
    expect(history.entries()).toEqual([
      expect.objectContaining({
        mode: 'quick',
        total: 3,
        correct: 2,
        topics: { java: { answered: 2, correct: 1 }, docker: { answered: 1, correct: 1 } },
      }),
    ]);

    session.next();
    expect(history.entries()).toHaveLength(1);
  });

  it('lists the mistakes with the chosen and the correct option texts', async () => {
    await startWith(['java-001', 'java'], ['java-002', 'java']);
    await session.answer('java-001-c');
    session.next();
    await session.answer('java-002-a');

    expect(session.mistakes()).toEqual([
      expect.objectContaining({
        question: expect.objectContaining({ id: 'java-001' }),
        chosenText: 'Option c',
        correctText: 'Option a',
      }),
    ]);
  });

  it('retries exactly the questions answered wrong', async () => {
    await startWith(['java-001', 'java'], ['java-002', 'java']);
    await session.answer('java-001-c');
    session.next();
    await session.answer('java-002-b');
    session.next();

    api.createQuiz.mockResolvedValue({ questions: [question('java-001'), question('java-002')] });
    await session.retryMistakes();

    expect(api.createQuiz).toHaveBeenLastCalledWith({ questionIds: ['java-001', 'java-002'] });
    expect(session.quizMode()).toBe('retry');
    expect(session.status()).toBe('playing');
    expect(session.answers().size).toBe(0);
  });
});

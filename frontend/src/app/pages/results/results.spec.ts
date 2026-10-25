import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { Results, verdict } from './results';
import { QuizApiService } from '../../core/quiz-api.service';
import { QuizSessionService } from '../../core/quiz-session.service';
import { ApiMock, apiMock, question, settle } from '../../../testing/fixtures';

describe('verdict', () => {
  it.each([
    [95, 'Excellent'],
    [70, 'Good job'],
    [50, 'Getting there'],
    [10, 'Keep practising'],
  ])('%i%% → %s', (percent, start) => {
    expect(verdict(percent)).toMatch(new RegExp(`^${start}`));
  });
});

describe('Results', () => {
  let api: ApiMock;
  let router: Router;
  let session: QuizSessionService;

  beforeEach(() => {
    localStorage.clear();
    api = apiMock();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: QuizApiService, useValue: api }],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    session = TestBed.inject(QuizSessionService);
  });

  /** Plays a quiz, answering each question with the given option letter. */
  async function play(answers: Record<string, string>) {
    const ids = Object.keys(answers);
    api.createQuiz.mockResolvedValue({
      questions: ids.map((id) => question(id, id.split('-')[0], id === 'java-001')),
    });
    await session.start({}, 'quick');
    for (const id of ids) {
      await session.answer(`${id}-${answers[id]}`);
      session.next();
    }
  }

  async function render() {
    const fixture = TestBed.createComponent(Results);
    await fixture.whenStable();
    return { fixture, host: fixture.nativeElement as HTMLElement };
  }

  it('shows the score, the verdict and the per-topic breakdown, worst first', async () => {
    await play({ 'java-001': 'b', 'docker-001': 'a', 'java-002': 'a' });
    const { host } = await render();

    expect(host.querySelector('h1')?.textContent).toContain('2 / 3');
    expect(host.querySelector('.percent')?.textContent).toBe('67%');
    expect(host.querySelector('.score p')?.textContent).toContain('Getting there');
    const rows = [...host.querySelectorAll('tbody tr')].map((row) => row.textContent?.trim());
    expect(rows).toEqual([
      expect.stringMatching(/Java\s*1 \/ 2/),
      expect.stringMatching(/Docker\s*1 \/ 1/),
    ]);
  });

  it('explains each mistake with both answers', async () => {
    await play({ 'java-001': 'c', 'docker-001': 'a' });
    const { host } = await render();

    const mistakes = host.querySelectorAll('.mistake');
    expect(mistakes).toHaveLength(1);
    expect(mistakes[0].querySelector('.wrong')?.textContent).toContain('Option c');
    expect(mistakes[0].querySelector('.right')?.textContent).toContain('Option a');
    expect(mistakes[0].querySelector('app-code-block')).not.toBeNull();
    expect(mistakes[0].textContent).toContain('Explanation of java-001');
    expect(host.querySelector('.score button')?.textContent).toContain('Retry my 1 mistake');
  });

  it('pluralises several mistakes', async () => {
    await play({ 'java-001': 'b', 'docker-001': 'b' });
    const { host } = await render();
    expect(host.querySelector('.score button')?.textContent).toContain('Retry my 2 mistakes');
  });

  it('congratulates a perfect score and offers no retry', async () => {
    await play({ 'java-001': 'a' });
    const { host } = await render();
    expect(host.textContent).toContain('No mistakes');
    expect(host.querySelector('.score button')).toBeNull();
  });

  it('retries the mistakes in a new quiz', async () => {
    await play({ 'java-001': 'b' });
    const { host } = await render();

    api.createQuiz.mockResolvedValue({ questions: [question('java-001')] });
    host.querySelector<HTMLButtonElement>('.score button')!.click();
    await settle();

    expect(api.createQuiz).toHaveBeenLastCalledWith({ questionIds: ['java-001'] });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/quiz');
  });

  it('stays on the results with an error if the retry fails', async () => {
    await play({ 'java-001': 'b' });
    const { fixture, host } = await render();

    api.createQuiz.mockRejectedValue(new Error('down'));
    host.querySelector<HTMLButtonElement>('.score button')!.click();
    await settle();
    await fixture.whenStable();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(host.querySelector('[role=alert]')).not.toBeNull();
  });
});

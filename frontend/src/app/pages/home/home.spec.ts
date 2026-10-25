import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { Home, RECENT_SESSIONS } from './home';
import { HistoryService } from '../../core/history.service';
import { QuizApiService } from '../../core/quiz-api.service';
import { ApiMock, apiMock, question, settle } from '../../../testing/fixtures';

describe('Home', () => {
  let api: ApiMock;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    api = apiMock();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: QuizApiService, useValue: api }],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  async function render() {
    const fixture = TestBed.createComponent(Home);
    await fixture.whenStable();
    return { fixture, host: fixture.nativeElement as HTMLElement };
  }

  const button = (host: HTMLElement, label: string) =>
    [...host.querySelectorAll('button')].find((b) => b.textContent?.includes(label))!;

  it('offers the quick mix sizes and the custom quiz, with no history yet', async () => {
    const { host } = await render();

    expect([...host.querySelectorAll('#quick-title ~ div button, .row button')].length).toBe(3);
    expect(host.querySelector('a[href="/custom"]')).not.toBeNull();
    expect(host.textContent).not.toContain('Recent sessions');
    expect(host.textContent).not.toContain('Topics to work on');
  });

  it('starts a quick mix and opens the quiz', async () => {
    api.createQuiz.mockResolvedValue({ questions: [question('java-001')] });
    const { host } = await render();

    button(host, '20 questions').click();
    await settle();

    expect(api.createQuiz).toHaveBeenCalledWith({ size: 20 });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/quiz');
  });

  it('stays home and shows the error when the quiz cannot be created', async () => {
    api.createQuiz.mockRejectedValue(new Error('down'));
    const { fixture, host } = await render();

    button(host, '10 questions').click();
    await settle();
    await fixture.whenStable();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(host.querySelector('[role=alert]')?.textContent).toContain('Could not load the quiz');
  });

  it('shows recent sessions and weak topics, and practises the weak ones', async () => {
    const history = TestBed.inject(HistoryService);
    for (let i = 0; i < RECENT_SESSIONS + 2; i++) {
      history.record({
        finishedAt: `2026-10-2${i}T10:00:00.000Z`,
        mode: 'custom',
        total: 4,
        correct: 1,
        topics: { docker: { answered: 4, correct: 1 } },
      });
    }
    api.createQuiz.mockResolvedValue({ questions: [question('docker-001', 'docker')] });
    const { host } = await render();

    expect(host.querySelectorAll('tbody tr')).toHaveLength(RECENT_SESSIONS);
    expect(host.querySelector('tbody td:nth-child(3)')?.textContent).toContain('1 / 4 (25%)');
    expect(host.querySelector('.weak')?.textContent).toContain('Docker');

    button(host, 'Practise these topics').click();
    await settle();
    expect(api.createQuiz).toHaveBeenCalledWith({ topicIds: ['docker'], size: 10 });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/quiz');
  });

  it('clears the history', async () => {
    TestBed.inject(HistoryService).record({
      finishedAt: '2026-10-25T10:00:00.000Z',
      mode: 'quick',
      total: 0,
      correct: 0,
      topics: {},
    });
    const { fixture, host } = await render();

    expect(host.querySelector('tbody td:nth-child(3)')?.textContent).toContain('(0%)');
    button(host, 'Clear history').click();
    await fixture.whenStable();
    expect(host.textContent).not.toContain('Recent sessions');
  });
});

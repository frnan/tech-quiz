import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { QuizPage } from './quiz';
import { QuizApiService } from '../../core/quiz-api.service';
import { QuizSessionService } from '../../core/quiz-session.service';
import { ApiMock, apiMock, question, settle } from '../../../testing/fixtures';

describe('QuizPage', () => {
  let api: ApiMock;
  let router: Router;
  let session: QuizSessionService;

  beforeEach(async () => {
    localStorage.clear();
    api = apiMock();
    api.createQuiz.mockResolvedValue({
      questions: [question('java-001', 'java', true), question('docker-001', 'docker')],
    });
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: QuizApiService, useValue: api }],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    session = TestBed.inject(QuizSessionService);
    await session.start({}, 'quick');
  });

  async function render() {
    const fixture = TestBed.createComponent(QuizPage);
    await fixture.whenStable();
    const host = fixture.nativeElement as HTMLElement;
    const options = () => [...host.querySelectorAll<HTMLButtonElement>('button.option')];
    const nextButton = () => host.querySelector<HTMLButtonElement>('.actions button')!;
    const stable = async () => {
      await settle();
      await fixture.whenStable();
    };
    return { fixture, host, options, nextButton, stable };
  }

  const key = (k: string, init: KeyboardEventInit = {}) =>
    document.dispatchEvent(new KeyboardEvent('keydown', { key: k, ...init }));

  it('shows the current question, its code and its options', async () => {
    const { host, options, nextButton } = await render();

    expect(host.querySelector('.meta')?.textContent).toContain('Question 1 of 2');
    expect(host.querySelector('.topic')?.textContent).toContain('Java');
    expect(host.querySelector('h1 code')?.textContent).toBe('code');
    expect(host.querySelector('app-code-block')).not.toBeNull();
    expect(options()).toHaveLength(3);
    expect(nextButton().disabled).toBe(true);
    expect(host.querySelector('[role=progressbar]')?.getAttribute('aria-valuenow')).toBe('0');
    expect(document.activeElement).toBe(host.querySelector('h1'));
  });

  it('answers on click and explains the result', async () => {
    const { host, options, nextButton, stable } = await render();

    options()[1].click();
    await stable();

    expect(api.answer).toHaveBeenCalledWith('java-001', 'java-001-b');
    expect(host.querySelector('.feedback h2')?.textContent).toBe('Not quite.');
    expect(host.querySelector('.feedback')?.textContent).toContain('Explanation of java-001');
    expect(host.querySelector('.refs a')?.getAttribute('href')).toBe('https://example.com/ref');
    expect(options().map((o) => o.className.replace('option', '').trim())).toEqual([
      'correct',
      'wrong',
      'dimmed',
    ]);
    expect(options().every((o) => o.disabled)).toBe(true);
    expect(document.activeElement).toBe(nextButton());
  });

  it('praises a correct answer', async () => {
    const { host, options, stable } = await render();
    options()[0].click();
    await stable();
    expect(host.querySelector('.feedback.correct h2')?.textContent).toBe('Correct!');
  });

  it('plays with the keyboard: digits answer, the right arrow continues', async () => {
    const { host, stable } = await render();

    key('9');
    key('1', { ctrlKey: true });
    key('ArrowRight');
    await stable();
    expect(api.answer).not.toHaveBeenCalled();

    key('3');
    await stable();
    expect(api.answer).toHaveBeenCalledWith('java-001', 'java-001-c');

    key('ArrowRight');
    await stable();
    expect(host.querySelector('.meta')?.textContent).toContain('Question 2 of 2');
  });

  it('shows "See results" on the last question and opens the results', async () => {
    const { options, nextButton, stable } = await render();
    options()[0].click();
    await stable();
    nextButton().click();
    await stable();

    expect(nextButton().textContent).toContain('See results');
    options()[0].click();
    await stable();
    nextButton().click();
    await stable();

    expect(session.status()).toBe('finished');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/results');
  });

  it('shows an answer that could not be checked', async () => {
    api.answer.mockRejectedValueOnce(new Error('offline'));
    const { host, options, stable } = await render();
    options()[0].click();
    await stable();
    expect(host.querySelector('[role=alert]')?.textContent).toContain('Could not check');
  });
});

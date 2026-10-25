import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { Custom } from './custom';
import { QuizApiService } from '../../core/quiz-api.service';
import { ApiMock, apiMock, question, settle } from '../../../testing/fixtures';

describe('Custom', () => {
  let api: ApiMock;
  let router: Router;

  beforeEach(() => {
    api = apiMock();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: QuizApiService, useValue: api }],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  async function render() {
    const fixture = TestBed.createComponent(Custom);
    await fixture.whenStable();
    const host = fixture.nativeElement as HTMLElement;
    const checkboxes = () => [...host.querySelectorAll<HTMLInputElement>('input[type=checkbox]')];
    const radios = () => [...host.querySelectorAll<HTMLInputElement>('input[type=radio]')];
    const button = (label: string) =>
      [...host.querySelectorAll('button')].find((b) => b.textContent?.includes(label))!;
    return { fixture, host, checkboxes, radios, button };
  }

  it('lists the topics with their counts, nothing selected', async () => {
    const { host, checkboxes, button } = await render();

    expect(checkboxes()).toHaveLength(2);
    expect(host.querySelector('.topic')?.textContent).toContain('Java');
    expect(host.querySelector('.count')?.textContent).toContain('10');
    expect(button('Start the quiz').disabled).toBe(true);
  });

  it('starts a quiz on the selected topics with the chosen size', async () => {
    api.createQuiz.mockResolvedValue({ questions: [question('docker-001', 'docker')] });
    const { fixture, host, checkboxes, radios, button } = await render();

    checkboxes()[1].click();
    radios()[2].click();
    await fixture.whenStable();
    expect(host.textContent).toContain('1 selected');
    expect(host.querySelector('.note')?.textContent).toContain('4 questions');

    button('Start the quiz').click();
    await settle();
    expect(api.createQuiz).toHaveBeenCalledWith({ topicIds: ['docker'], size: 30 });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/quiz');
  });

  it('selects all, unselects one, and clears', async () => {
    const { fixture, host, checkboxes, button } = await render();

    button('Select all').click();
    await fixture.whenStable();
    expect(checkboxes().every((box) => box.checked)).toBe(true);

    checkboxes()[0].click();
    await fixture.whenStable();
    expect(host.textContent).toContain('1 selected');

    button('Clear').click();
    await fixture.whenStable();
    expect(checkboxes().some((box) => box.checked)).toBe(false);
  });

  it('does not start without a topic', async () => {
    const { host } = await render();
    host.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();
    expect(api.createQuiz).not.toHaveBeenCalled();
  });

  it('stays on the page with an error if the quiz fails', async () => {
    api.createQuiz.mockResolvedValue({ questions: [] });
    const { fixture, host, checkboxes, button } = await render();
    checkboxes()[0].click();
    await fixture.whenStable();

    button('Start the quiz').click();
    await settle();
    await fixture.whenStable();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(host.querySelector('[role=alert]')?.textContent).toContain('No question');
  });

  it('offers a retry when the topics cannot be loaded', async () => {
    api.topics.mockRejectedValueOnce(new Error('down'));
    const { fixture, host, button } = await render();
    await settle();
    await fixture.whenStable();
    expect(host.querySelector('[role=alert]')?.textContent).toContain('Could not load the topics');

    button('Try again').click();
    await settle();
    await fixture.whenStable();
    expect(host.querySelectorAll('input[type=checkbox]')).toHaveLength(2);
  });
});

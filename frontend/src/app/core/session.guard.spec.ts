import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';

import { QuizApiService } from './quiz-api.service';
import { QuizSessionService } from './quiz-session.service';
import { sessionGuard } from './session.guard';
import { apiMock, question } from '../../testing/fixtures';

describe('sessionGuard', () => {
  const run = () =>
    TestBed.runInInjectionContext(() =>
      sessionGuard('playing')({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => {
    const api = apiMock();
    api.createQuiz.mockResolvedValue({ questions: [question('java-001')] });
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: QuizApiService, useValue: api }],
    });
  });

  it('sends the user home without a session in the expected state', () => {
    const result = run();
    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toBe('/');
  });

  it('lets the user in when the session is in the expected state', async () => {
    await TestBed.inject(QuizSessionService).start({}, 'quick');
    expect(run()).toBe(true);
  });
});

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { QuizSessionService, SessionStatus } from './quiz-session.service';

/** The quiz and results pages need a session in the matching state; otherwise back home. */
export function sessionGuard(...allowed: SessionStatus[]): CanActivateFn {
  return () =>
    allowed.includes(inject(QuizSessionService).status()) || inject(Router).createUrlTree(['/']);
}

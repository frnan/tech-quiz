import { Routes } from '@angular/router';

import { sessionGuard } from './core/session.guard';

export const routes: Routes = [
  {
    path: '',
    title: 'Tech Quiz',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
  },
  {
    path: 'custom',
    title: 'Custom quiz · Tech Quiz',
    loadComponent: () => import('./pages/custom/custom').then((m) => m.Custom),
  },
  {
    path: 'quiz',
    title: 'Quiz · Tech Quiz',
    canActivate: [sessionGuard('playing')],
    loadComponent: () => import('./pages/quiz/quiz').then((m) => m.QuizPage),
  },
  {
    path: 'results',
    title: 'Results · Tech Quiz',
    canActivate: [sessionGuard('finished')],
    loadComponent: () => import('./pages/results/results').then((m) => m.Results),
  },
  { path: '**', redirectTo: '' },
];

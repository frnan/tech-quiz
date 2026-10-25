import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AnswerResult, Quiz, QuizRequest, Topic } from './models';

/** The backend's quiz API. Same origin in production (ADR 0003); proxied by `ng serve` in dev. */
@Service()
export class QuizApiService {
  private readonly http = inject(HttpClient);

  topics(): Promise<Topic[]> {
    return firstValueFrom(this.http.get<Topic[]>('/api/v1/topics'));
  }

  createQuiz(request: QuizRequest): Promise<Quiz> {
    return firstValueFrom(this.http.post<Quiz>('/api/v1/quizzes', request));
  }

  answer(questionId: string, optionId: string): Promise<AnswerResult> {
    return firstValueFrom(
      this.http.post<AnswerResult>(`/api/v1/questions/${encodeURIComponent(questionId)}/answer`, {
        optionId,
      }),
    );
  }
}

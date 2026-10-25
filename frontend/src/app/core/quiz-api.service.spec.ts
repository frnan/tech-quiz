import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { QuizApiService } from './quiz-api.service';
import { TOPICS, question, result } from '../../testing/fixtures';

describe('QuizApiService', () => {
  let api: QuizApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(QuizApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists topics', async () => {
    const topics = api.topics();
    http.expectOne({ method: 'GET', url: '/api/v1/topics' }).flush(TOPICS);
    expect(await topics).toEqual(TOPICS);
  });

  it('creates a quiz from the request', async () => {
    const quiz = api.createQuiz({ topicIds: ['java'], size: 10 });
    const req = http.expectOne({ method: 'POST', url: '/api/v1/quizzes' });
    expect(req.request.body).toEqual({ topicIds: ['java'], size: 10 });
    req.flush({ questions: [question('java-001')] });
    expect((await quiz).questions).toHaveLength(1);
  });

  it('posts an answer, encoding the question id', async () => {
    const answer = api.answer('a/b', 'o1');
    const req = http.expectOne({ method: 'POST', url: '/api/v1/questions/a%2Fb/answer' });
    expect(req.request.body).toEqual({ optionId: 'o1' });
    req.flush(result('a/b', 'o1'));
    expect((await answer).correct).toBe(false);
  });

  it('rejects on an HTTP error', async () => {
    const topics = api.topics();
    http.expectOne('/api/v1/topics').flush(null, { status: 500, statusText: 'Server Error' });
    await expect(topics).rejects.toBeTruthy();
  });
});

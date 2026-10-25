import { TestBed } from '@angular/core/testing';

import { QuizApiService } from './quiz-api.service';
import { TopicsService } from './topics.service';
import { ApiMock, TOPICS, apiMock } from '../../testing/fixtures';

describe('TopicsService', () => {
  let api: ApiMock;
  let service: TopicsService;

  beforeEach(() => {
    api = apiMock();
    TestBed.configureTestingModule({ providers: [{ provide: QuizApiService, useValue: api }] });
    service = TestBed.inject(TopicsService);
  });

  it('loads the topics once, even with concurrent callers', async () => {
    await Promise.all([service.load(), service.load()]);
    await service.load();

    expect(api.topics).toHaveBeenCalledTimes(1);
    expect(service.topics()).toEqual(TOPICS);
    expect(service.loadFailed()).toBe(false);
  });

  it('flags a failure and lets the caller retry', async () => {
    api.topics.mockRejectedValueOnce(new Error('down'));
    await service.load();
    expect(service.loadFailed()).toBe(true);
    expect(service.topics()).toEqual([]);

    await service.load();
    expect(api.topics).toHaveBeenCalledTimes(2);
    expect(service.loadFailed()).toBe(false);
  });

  it('gives a topic name, falling back to its id', async () => {
    expect(service.nameOf('java')).toBe('java');
    await service.load();
    expect(service.nameOf('java')).toBe('Java');
    expect(service.nameOf('removed')).toBe('removed');
  });
});

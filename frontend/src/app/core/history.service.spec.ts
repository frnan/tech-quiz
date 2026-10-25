import { TestBed } from '@angular/core/testing';

import { HISTORY_KEY, HistoryEntry, HistoryService, MAX_ENTRIES } from './history.service';

function entry(
  topics: HistoryEntry['topics'],
  finishedAt = '2026-10-25T10:00:00.000Z',
): HistoryEntry {
  const values = Object.values(topics);
  return {
    finishedAt,
    mode: 'quick',
    total: values.reduce((sum, score) => sum + score.answered, 0),
    correct: values.reduce((sum, score) => sum + score.correct, 0),
    topics,
  };
}

describe('HistoryService', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  const create = () => TestBed.inject(HistoryService);

  it('starts empty and persists recorded entries, most recent first', () => {
    const history = create();
    expect(history.entries()).toEqual([]);

    const first = entry({ java: { answered: 2, correct: 1 } }, '2026-10-25T09:00:00.000Z');
    const second = entry({ java: { answered: 1, correct: 1 } });
    history.record(first);
    history.record(second);

    expect(history.entries()).toEqual([second, first]);
    expect(JSON.parse(localStorage.getItem(HISTORY_KEY)!)).toEqual([second, first]);
  });

  it('reads what a previous visit stored, dropping malformed entries', () => {
    const stored = entry({ docker: { answered: 3, correct: 3 } });
    localStorage.setItem(HISTORY_KEY, JSON.stringify([stored, { nope: true }, null, 'x']));

    expect(create().entries()).toEqual([stored]);
  });

  it('ignores corrupted or non-array storage', () => {
    localStorage.setItem(HISTORY_KEY, '{not json');
    expect(create().entries()).toEqual([]);

    TestBed.resetTestingModule();
    localStorage.setItem(HISTORY_KEY, '{"a":1}');
    expect(create().entries()).toEqual([]);
  });

  it('keeps working when storage throws', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota');
    });
    const history = create();

    history.record(entry({ java: { answered: 1, correct: 0 } }));
    expect(history.entries()).toHaveLength(1);
  });

  it('keeps only the most recent entries', () => {
    const history = create();
    for (let i = 0; i < MAX_ENTRIES + 5; i++) {
      history.record(entry({ java: { answered: 1, correct: 1 } }, `2026-10-25T10:00:${i}`));
    }
    expect(history.entries()).toHaveLength(MAX_ENTRIES);
    expect(history.entries()[0].finishedAt).toBe(`2026-10-25T10:00:${MAX_ENTRIES + 4}`);
  });

  it('computes accuracy per topic, worst first, and the weak topics', () => {
    const history = create();
    history.record(
      entry({ java: { answered: 4, correct: 1 }, docker: { answered: 2, correct: 0 } }),
    );
    history.record(
      entry({ java: { answered: 4, correct: 4 }, angular: { answered: 5, correct: 1 } }),
    );

    expect(history.topicStats().map((stat) => [stat.topicId, stat.accuracy])).toEqual([
      ['docker', 0],
      ['angular', 0.2],
      ['java', 5 / 8],
    ]);
    // docker has too few answers to be judged; java is above the threshold.
    expect(history.weakTopics().map((stat) => stat.topicId)).toEqual(['angular']);
  });

  it('sorts topics with the same accuracy by id', () => {
    const history = create();
    history.record(entry({ b: { answered: 1, correct: 1 }, a: { answered: 1, correct: 1 } }));
    expect(history.topicStats().map((stat) => stat.topicId)).toEqual(['a', 'b']);
  });

  it('clears everything', () => {
    const history = create();
    history.record(entry({ java: { answered: 1, correct: 1 } }));
    history.clear();
    expect(history.entries()).toEqual([]);
    expect(localStorage.getItem(HISTORY_KEY)).toBe('[]');
  });
});

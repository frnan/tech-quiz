import { Service, computed, signal } from '@angular/core';

import { QuizMode } from './models';

export interface TopicScore {
  readonly answered: number;
  readonly correct: number;
}

/** A finished quiz, as kept in the browser (ADR 0004: nothing is stored server-side). */
export interface HistoryEntry {
  readonly finishedAt: string;
  readonly mode: QuizMode;
  readonly total: number;
  readonly correct: number;
  readonly topics: Readonly<Record<string, TopicScore>>;
}

export interface TopicStat extends TopicScore {
  readonly topicId: string;
  readonly accuracy: number;
}

export const HISTORY_KEY = 'tech-quiz.history.v1';
export const MAX_ENTRIES = 50;
/** A topic needs this many answers before it can be called weak. */
export const WEAK_TOPIC_MIN_ANSWERS = 3;
export const WEAK_TOPIC_MAX_ACCURACY = 0.6;

/**
 * Past sessions in localStorage. Every access is guarded: storage can be missing, full or
 * blocked (private windows), and the app must keep working without it.
 */
@Service()
export class HistoryService {
  private readonly items = signal<readonly HistoryEntry[]>(read());

  /** Most recent first. */
  readonly entries = this.items.asReadonly();

  /** Accuracy per topic over the whole history, worst first. */
  readonly topicStats = computed<TopicStat[]>(() => {
    const totals = new Map<string, { answered: number; correct: number }>();
    for (const entry of this.items()) {
      for (const [topicId, score] of Object.entries(entry.topics)) {
        const total = totals.get(topicId) ?? { answered: 0, correct: 0 };
        total.answered += score.answered;
        total.correct += score.correct;
        totals.set(topicId, total);
      }
    }
    return [...totals.entries()]
      .map(([topicId, total]) => ({ topicId, ...total, accuracy: total.correct / total.answered }))
      .sort((a, b) => a.accuracy - b.accuracy || a.topicId.localeCompare(b.topicId));
  });

  readonly weakTopics = computed(() =>
    this.topicStats().filter(
      (stat) => stat.answered >= WEAK_TOPIC_MIN_ANSWERS && stat.accuracy < WEAK_TOPIC_MAX_ACCURACY,
    ),
  );

  record(entry: HistoryEntry): void {
    this.items.update((entries) => [entry, ...entries].slice(0, MAX_ENTRIES));
    write(this.items());
  }

  clear(): void {
    this.items.set([]);
    write([]);
  }
}

function read(): readonly HistoryEntry[] {
  try {
    const raw = localStorage.getItem(HISTORY_KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed.filter(isEntry) : [];
  } catch {
    return [];
  }
}

function write(entries: readonly HistoryEntry[]): void {
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(entries));
  } catch {
    // Storage unavailable or full: history just isn't kept.
  }
}

function isEntry(value: unknown): value is HistoryEntry {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const entry = value as Partial<HistoryEntry>;
  return (
    typeof entry.finishedAt === 'string' &&
    typeof entry.total === 'number' &&
    typeof entry.correct === 'number' &&
    typeof entry.topics === 'object' &&
    entry.topics !== null
  );
}

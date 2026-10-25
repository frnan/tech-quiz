import { Service, computed, inject, signal } from '@angular/core';

import { Topic } from './models';
import { QuizApiService } from './quiz-api.service';

/** The topic list, fetched once and shared by every page. */
@Service()
export class TopicsService {
  private readonly api = inject(QuizApiService);

  private readonly list = signal<readonly Topic[]>([]);
  private readonly failed = signal(false);
  private loading: Promise<void> | null = null;

  readonly topics = this.list.asReadonly();
  readonly loadFailed = this.failed.asReadonly();
  private readonly byId = computed(() => new Map(this.list().map((topic) => [topic.id, topic])));

  /** Loads the topics unless they're already loaded or loading. A failure can be retried. */
  load(): Promise<void> {
    this.loading ??= this.api.topics().then(
      (topics) => {
        this.list.set(topics);
        this.failed.set(false);
      },
      () => {
        this.failed.set(true);
        this.loading = null;
      },
    );
    return this.loading;
  }

  /** The topic's display name, or its id if the topics aren't loaded (or it was removed). */
  nameOf(topicId: string): string {
    return this.byId().get(topicId)?.name ?? topicId;
  }
}

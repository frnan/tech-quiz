import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { QUIZ_SIZES } from '../../core/models';
import { QuizSessionService } from '../../core/quiz-session.service';
import { TopicsService } from '../../core/topics.service';

@Component({
  selector: 'app-custom',
  templateUrl: './custom.html',
  styleUrl: './custom.scss',
})
export class Custom {
  private readonly router = inject(Router);
  protected readonly session = inject(QuizSessionService);
  protected readonly topics = inject(TopicsService);

  protected readonly sizes = QUIZ_SIZES;
  protected readonly selected = signal<ReadonlySet<string>>(new Set());
  protected readonly size = signal<number>(QUIZ_SIZES[0]);

  protected readonly available = computed(() =>
    this.topics
      .topics()
      .filter((topic) => this.selected().has(topic.id))
      .reduce((sum, topic) => sum + topic.questionCount, 0),
  );
  protected readonly canStart = computed(
    () => this.selected().size > 0 && this.session.status() !== 'loading',
  );

  constructor() {
    void this.topics.load();
  }

  protected toggle(topicId: string): void {
    this.selected.update((selected) => {
      const next = new Set(selected);
      if (!next.delete(topicId)) {
        next.add(topicId);
      }
      return next;
    });
  }

  protected selectAll(): void {
    this.selected.set(new Set(this.topics.topics().map((topic) => topic.id)));
  }

  protected clear(): void {
    this.selected.set(new Set());
  }

  protected retryLoad(): void {
    void this.topics.load();
  }

  protected async start(): Promise<void> {
    if (!this.canStart()) {
      return;
    }
    await this.session.start({ topicIds: [...this.selected()], size: this.size() }, 'custom');
    if (this.session.status() === 'playing') {
      await this.router.navigateByUrl('/quiz');
    }
  }
}

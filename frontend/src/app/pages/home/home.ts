import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { HistoryService } from '../../core/history.service';
import { QUIZ_SIZES, QuizMode } from '../../core/models';
import { QuizSessionService } from '../../core/quiz-session.service';
import { TopicsService } from '../../core/topics.service';

export const RECENT_SESSIONS = 5;
const MODE_LABELS: Record<QuizMode, string> = {
  quick: 'Quick mix',
  custom: 'Custom',
  retry: 'Retry',
};

@Component({
  selector: 'app-home',
  imports: [RouterLink, DatePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly router = inject(Router);
  protected readonly session = inject(QuizSessionService);
  protected readonly history = inject(HistoryService);
  protected readonly topics = inject(TopicsService);

  protected readonly sizes = QUIZ_SIZES;
  protected readonly modeLabels = MODE_LABELS;
  protected readonly recent = computed(() => this.history.entries().slice(0, RECENT_SESSIONS));
  protected readonly loading = computed(() => this.session.status() === 'loading');

  constructor() {
    void this.topics.load();
  }

  protected quickMix(size: number): Promise<void> {
    return this.startAndPlay(() => this.session.start({ size }, 'quick'));
  }

  protected practiceWeakTopics(): Promise<void> {
    const topicIds = this.history.weakTopics().map((stat) => stat.topicId);
    return this.startAndPlay(() => this.session.start({ topicIds, size: 10 }, 'custom'));
  }

  protected percent(correct: number, total: number): number {
    return total ? Math.round((100 * correct) / total) : 0;
  }

  private async startAndPlay(start: () => Promise<void>): Promise<void> {
    await start();
    if (this.session.status() === 'playing') {
      await this.router.navigateByUrl('/quiz');
    }
  }
}

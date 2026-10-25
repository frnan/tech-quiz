import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { QuizSessionService } from '../../core/quiz-session.service';
import { TopicsService } from '../../core/topics.service';
import { CodeBlock } from '../../shared/code-block/code-block';
import { RichText } from '../../shared/rich-text/rich-text';

/** Score bands for the headline. */
export function verdict(percent: number): string {
  if (percent >= 90) {
    return 'Excellent, interview-ready on these topics.';
  }
  if (percent >= 70) {
    return 'Good job. Review the mistakes below to close the gaps.';
  }
  if (percent >= 50) {
    return 'Getting there. The explanations below are worth a careful read.';
  }
  return 'Keep practising. Retry your mistakes after reading the explanations.';
}

@Component({
  selector: 'app-results',
  imports: [RouterLink, CodeBlock, RichText],
  templateUrl: './results.html',
  styleUrl: './results.scss',
})
export class Results {
  private readonly router = inject(Router);
  protected readonly session = inject(QuizSessionService);
  protected readonly topics = inject(TopicsService);

  protected readonly verdict = computed(() => verdict(this.session.scorePercent()));
  protected readonly perTopic = computed(() =>
    [...this.session.topicScores().entries()]
      .map(([topicId, score]) => ({ topicId, ...score }))
      .sort((a, b) => a.correct / a.answered - b.correct / b.answered),
  );

  constructor() {
    void this.topics.load();
  }

  protected async retryMistakes(): Promise<void> {
    await this.session.retryMistakes();
    if (this.session.status() === 'playing') {
      await this.router.navigateByUrl('/quiz');
    }
  }
}

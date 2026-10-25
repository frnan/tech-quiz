import {
  Component,
  ElementRef,
  afterRenderEffect,
  computed,
  inject,
  untracked,
  viewChild,
} from '@angular/core';
import { Router } from '@angular/router';

import { QuizOption } from '../../core/models';
import { QuizSessionService } from '../../core/quiz-session.service';
import { TopicsService } from '../../core/topics.service';
import { CodeBlock } from '../../shared/code-block/code-block';
import { RichText } from '../../shared/rich-text/rich-text';

type OptionState = 'idle' | 'correct' | 'wrong' | 'dimmed';

@Component({
  selector: 'app-quiz',
  imports: [CodeBlock, RichText],
  templateUrl: './quiz.html',
  styleUrl: './quiz.scss',
  host: { '(document:keydown)': 'onKey($event)' },
})
export class QuizPage {
  private readonly router = inject(Router);
  protected readonly session = inject(QuizSessionService);
  protected readonly topics = inject(TopicsService);

  private readonly heading = viewChild<ElementRef<HTMLElement>>('heading');
  private readonly nextButton = viewChild<ElementRef<HTMLButtonElement>>('nextButton');

  protected readonly progress = computed(() =>
    Math.round((100 * this.session.index()) / Math.max(this.session.questions().length, 1)),
  );

  constructor() {
    void this.topics.load();
    // After rendering, so the elements are up to date (e.g. "Next" is enabled).
    // A new question: focus its heading so screen readers announce it.
    afterRenderEffect(() => {
      this.session.index();
      untracked(() => this.heading()?.nativeElement.focus());
    });
    // Answered: focus "Next" so Enter continues.
    afterRenderEffect(() => {
      if (this.session.currentAnswer()) {
        untracked(() => this.nextButton()?.nativeElement.focus());
      }
    });
  }

  protected optionState(option: QuizOption): OptionState {
    const answer = this.session.currentAnswer();
    if (!answer) {
      return 'idle';
    }
    if (option.id === answer.result.correctOptionId) {
      return 'correct';
    }
    return option.id === answer.optionId ? 'wrong' : 'dimmed';
  }

  protected choose(option: QuizOption): Promise<void> {
    return this.session.answer(option.id);
  }

  protected async next(): Promise<void> {
    this.session.next();
    if (this.session.status() === 'finished') {
      await this.router.navigateByUrl('/results');
    }
  }

  /** 1-5 picks an option; Enter or → goes on once answered. */
  protected onKey(event: KeyboardEvent): void {
    if (event.altKey || event.ctrlKey || event.metaKey) {
      return;
    }
    const question = this.session.current();
    const digit = Number(event.key);
    if (question && Number.isInteger(digit) && digit >= 1 && digit <= question.options.length) {
      event.preventDefault();
      void this.choose(question.options[digit - 1]);
    } else if (event.key === 'ArrowRight' && this.session.currentAnswer()) {
      event.preventDefault();
      void this.next();
    }
  }
}

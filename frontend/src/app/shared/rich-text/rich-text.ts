import { Component, computed, input } from '@angular/core';

export interface TextPart {
  readonly text: string;
  readonly code: boolean;
}

/** Splits on backticks: `like this` becomes code. Unbalanced backticks stay as text. */
export function parseInlineCode(text: string): TextPart[] {
  const pieces = text.split('`');
  if (pieces.length % 2 === 0) {
    return [{ text, code: false }];
  }
  return pieces
    .map((piece, index) => ({ text: piece, code: index % 2 === 1 }))
    .filter((part) => part.text.length > 0);
}

/**
 * Question text with `inline code`. Rendered as text and <code> nodes, never as HTML, so
 * nothing in a question file can inject markup.
 */
@Component({
  selector: 'app-rich-text',
  templateUrl: './rich-text.html',
  styleUrl: './rich-text.scss',
})
export class RichText {
  readonly text = input.required<string>();
  protected readonly parts = computed(() => parseInlineCode(this.text()));
}

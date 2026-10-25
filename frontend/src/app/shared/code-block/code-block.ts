import { Component, computed, input } from '@angular/core';

import { CodeSnippet } from '../../core/models';
import { highlight } from './highlight';

@Component({
  selector: 'app-code-block',
  templateUrl: './code-block.html',
  styleUrl: './code-block.scss',
})
export class CodeBlock {
  readonly code = input.required<CodeSnippet>();
  protected readonly html = computed(() => highlight(this.code().source, this.code().language));
}

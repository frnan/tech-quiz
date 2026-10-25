import { TestBed } from '@angular/core/testing';

import { CodeBlock } from './code-block';
import { highlight } from './highlight';

describe('highlight', () => {
  it('highlights a registered language', () => {
    expect(highlight('public class A {}', 'java')).toContain('hljs-keyword');
  });

  it('accepts html as an alias of xml', () => {
    expect(highlight('<p>x</p>', 'html')).toContain('hljs-tag');
  });

  it('falls back to plain text for an unknown language, still escaped', () => {
    expect(highlight('<script>', 'cobol')).toBe('&lt;script&gt;');
  });
});

describe('CodeBlock', () => {
  it('renders the highlighted source with an accessible label', async () => {
    const fixture = TestBed.createComponent(CodeBlock);
    fixture.componentRef.setInput('code', { language: 'java', source: 'int x = 1; // <b>' });
    await fixture.whenStable();

    const pre: HTMLElement = fixture.nativeElement.querySelector('pre');
    expect(pre.getAttribute('aria-label')).toBe('java code');
    expect(pre.querySelector('.hljs-comment')?.textContent).toBe('// <b>');
    expect(pre.querySelector('b')).toBeNull();
  });
});

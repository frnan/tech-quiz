import { TestBed } from '@angular/core/testing';

import { RichText, parseInlineCode } from './rich-text';

describe('parseInlineCode', () => {
  it('turns backtick spans into code parts', () => {
    expect(parseInlineCode('Use `final` here')).toEqual([
      { text: 'Use ', code: false },
      { text: 'final', code: true },
      { text: ' here', code: false },
    ]);
  });

  it('keeps unbalanced backticks as plain text', () => {
    expect(parseInlineCode('a ` b')).toEqual([{ text: 'a ` b', code: false }]);
  });

  it('handles text without code', () => {
    expect(parseInlineCode('plain')).toEqual([{ text: 'plain', code: false }]);
  });
});

describe('RichText', () => {
  it('renders code spans as <code> and never interprets HTML', async () => {
    const fixture = TestBed.createComponent(RichText);
    fixture.componentRef.setInput('text', 'Call `<b>x()</b>` then <i>stop</i>');
    await fixture.whenStable();

    const host: HTMLElement = fixture.nativeElement;
    expect(host.querySelector('code')?.textContent).toBe('<b>x()</b>');
    expect(host.querySelector('b, i')).toBeNull();
    expect(host.textContent).toContain('then <i>stop</i>');
  });
});

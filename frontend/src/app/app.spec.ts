import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';

describe('App', () => {
  it('renders the header navigation and the outlet', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const host: HTMLElement = fixture.nativeElement;
    const links = [...host.querySelectorAll('nav a')].map((a) => a.textContent?.trim());
    expect(links).toEqual(['Home', 'Custom quiz']);
    expect(host.querySelector('router-outlet')).not.toBeNull();
  });
});

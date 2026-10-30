import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { AppComponent } from './app';
describe('App', () => {
  it('renders the existing application shell', async () => {
    await TestBed.configureTestingModule({ imports: [AppComponent], providers: [provideRouter([]), provideHttpClient()] }).compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-sidebar').textContent).toContain('EduEnroll');
    expect(fixture.nativeElement.querySelector('router-outlet')).toBeTruthy();
  });
});

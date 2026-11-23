import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    @if (authService.isAuthenticated()) {
      <header class="header">
        <div class="header-left">
          <a routerLink="/dashboard" class="home-btn">
            🏠 BRP Sistema
          </a>
        </div>
        <h1>Sistema de Ofertas BRP</h1>
        <div class="user-info">
          <span>{{ authService.getUsername() }} ({{ authService.getUserRole() }})</span>
          <button class="logout-btn" (click)="logout()">Cerrar Sesión</button>
        </div>
      </header>
    }
    <div class="container">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .header { display: flex; align-items: center; justify-content: space-between; padding: 10px 20px; background: #2c3e50; color: white; }
    .header h1 { margin: 0; font-size: 18px; }
    .header-left { display: flex; align-items: center; }
    .home-btn { color: white; text-decoration: none; font-size: 18px; font-weight: bold; display: flex; align-items: center; gap: 8px; padding: 8px 15px; border-radius: 4px; transition: background 0.2s; }
    .home-btn:hover { background: rgba(255,255,255,0.1); }
    .user-info { display: flex; align-items: center; gap: 15px; }
    .user-info span { font-size: 14px; opacity: 0.9; }
    .logout-btn { padding: 8px 15px; background: #e74c3c; color: white; border: none; border-radius: 4px; cursor: pointer; }
    .logout-btn:hover { background: #c0392b; }
    .container { padding: 20px; max-width: 1200px; margin: 0 auto; }
  `]
})
export class AppComponent {
  constructor(public authService: AuthService) {}

  logout(): void {
    this.authService.logout();
    window.location.href = '/login';
  }
}
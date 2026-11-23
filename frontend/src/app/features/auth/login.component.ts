import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <h2>Iniciar Sesión</h2>
        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="username">Usuario</label>
            <input
              type="text"
              id="username"
              [(ngModel)]="username"
              name="username"
              required
              placeholder="Ingrese su usuario">
          </div>
          <div class="form-group">
            <label for="password">Contraseña</label>
            <input
              type="password"
              id="password"
              [(ngModel)]="password"
              name="password"
              required
              placeholder="Ingrese su contraseña">
          </div>
          @if (error()) {
            <div class="error-message">{{ error() }}</div>
          }
          <button type="submit" class="btn btn-primary" [disabled]="loading()">
            {{ loading() ? 'Ingresando...' : 'Iniciar Sesión' }}
          </button>
        </form>
        <div class="demo-credentials">
          <h4>Credenciales de prueba:</h4>
          <p><strong>Admin:</strong> admin / admin123</p>
          <p><strong>Radicador:</strong> radicador1 / admin123</p>
          <p><strong>Analista:</strong> analista1 / admin123</p>
          <p><strong>Jefe:</strong> jefe1 / admin123</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 80vh;
    }
    .login-card {
      background: white;
      padding: 40px;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
      width: 100%;
      max-width: 400px;
    }
    .login-card h2 {
      text-align: center;
      margin-bottom: 30px;
      color: #333;
    }
    .login-card button {
      width: 100%;
      margin-top: 20px;
    }
    .demo-credentials {
      margin-top: 30px;
      padding: 15px;
      background: #f8f9fa;
      border-radius: 4px;
      font-size: 13px;
    }
    .demo-credentials h4 {
      margin-bottom: 10px;
      color: #666;
    }
    .demo-credentials p {
      margin: 5px 0;
      color: #555;
    }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  loading = signal(false);
  error = signal('');

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    if (!this.username || !this.password) {
      this.error.set('Por favor ingrese usuario y contraseña');
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Error al iniciar sesión');
      }
    });
  }
}
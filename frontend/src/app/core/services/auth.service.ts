import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  usuario: {
    id: number;
    username: string;
    nombre: string;
    rol: string;
    modulos: string[];
  };
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api';

  private tokenSignal = signal<string | null>(localStorage.getItem('token'));
  private userSignal = signal<LoginResponse['usuario'] | null>(null);

  token = computed(() => this.tokenSignal());
  user = computed(() => this.userSignal());

  constructor(private http: HttpClient) {
    this.loadFromStorage();
  }

  private loadFromStorage(): void {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('user');
    if (token) this.tokenSignal.set(token);
    if (userStr) {
      try {
        this.userSignal.set(JSON.parse(userStr));
      } catch {}
    }
  }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, { username, password }).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('user', JSON.stringify(response.usuario));
        this.tokenSignal.set(response.token);
        this.userSignal.set(response.usuario);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.tokenSignal.set(null);
    this.userSignal.set(null);
  }

  isAuthenticated(): boolean {
    return !!this.token();
  }

  getToken(): string | null {
    return this.token();
  }

  getUserRole(): string {
    return this.user()?.rol || '';
  }

  hasModule(modulo: string): boolean {
    return this.user()?.modulos?.includes(modulo) || false;
  }

  getUsername(): string {
    return this.user()?.username || '';
  }
}
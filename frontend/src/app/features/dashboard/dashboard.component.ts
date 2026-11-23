import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { BrpService, CatalogUpdateResponse } from '../../core/services/brp.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <h2>Bienvenido, {{ authService.getUsername() }}</h2>
    <p class="subtitle">Su rol: <strong>{{ authService.getUserRole() }}</strong></p>

    <div class="nav-menu">
      @if (authService.hasModule('RADICACION')) {
        <a routerLink="/radicar" routerLinkActive="active">Radicar Oferta</a>
      }
      @if (authService.hasModule('ANALISIS')) {
        <a routerLink="/analisis" routerLinkActive="active">Bandeja de Análisis</a>
      }
      @if (authService.hasModule('APROBACION')) {
        <a routerLink="/aprobacion" routerLinkActive="active">Bandeja de Aprobación</a>
      }
      @if (authService.hasModule('CATALOGO')) {
        <a routerLink="/catalogo" routerLinkActive="active">Catálogo BRP</a>
      }
      @if (authService.hasModule('SEGUIMIENTO')) {
        <a routerLink="/seguimiento" routerLinkActive="active">Seguimiento</a>
      }
      @if (authService.hasModule('ADMINISTRACION')) {
        <a routerLink="/administracion" routerLinkActive="active">Administración</a>
      }
    </div>

    <div class="card catalog-info">
      <h3>Información del Catálogo</h3>
      @if (catalogInfo()) {
        <div class="info-grid">
          <div class="info-item">
            <span class="label">Última actualización:</span>
            <span class="value">{{ catalogInfo()!.ultimaActualizacion }}</span>
          </div>
          <div class="info-item">
            <span class="label">Total BRP:</span>
            <span class="value">{{ catalogInfo()!.totalBrp }}</span>
          </div>
          <div class="info-item">
            <span class="label">Disponibles:</span>
            <span class="value">{{ catalogInfo()!.disponibles }}</span>
          </div>
        </div>
      } @else {
        <p>Cargando información...</p>
      }
    </div>

    <div class="card quick-actions">
      <h3>Acciones Rápidas</h3>
      <div class="actions-grid">
        @if (authService.hasModule('RADICACION')) {
          <a routerLink="/radicar" class="action-card">
            <h4>📝 Radicar Oferta</h4>
            <p>Crear nueva oferta sobre un BRP disponible</p>
          </a>
        }
        @if (authService.hasModule('ANALISIS')) {
          <a routerLink="/analisis" class="action-card">
            <h4>📋 Bandeja de Análisis</h4>
            <p>Tomar y procesar ofertas radicadas</p>
          </a>
        }
        @if (authService.hasModule('APROBACION')) {
          <a routerLink="/aprobacion" class="action-card">
            <h4>✅ Bandeja de Aprobación</h4>
            <p>Revisar y aprobar ofertas escaladas</p>
          </a>
        }
        @if (authService.hasModule('CATALOGO')) {
          <a routerLink="/catalogo" class="action-card">
            <h4>🏠 Catálogo BRP</h4>
            <p>Ver todos los bienes disponibles</p>
          </a>
        }
        @if (authService.hasModule('SEGUIMIENTO')) {
          <a routerLink="/seguimiento" class="action-card">
            <h4>📊 Seguimiento</h4>
            <p>Consultar estado de ofertas</p>
          </a>
        }
      </div>
    </div>
  `,
  styles: [`
    h2 { margin-bottom: 5px; }
    .subtitle { color: #666; margin-bottom: 20px; }
    .catalog-info { margin-bottom: 20px; }
    .info-grid { display: flex; gap: 30px; margin-top: 15px; }
    .info-item { display: flex; flex-direction: column; }
    .info-item .label { font-size: 12px; color: #666; }
    .info-item .value { font-size: 18px; font-weight: bold; color: #007bff; }
    .actions-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 15px; margin-top: 15px; }
    .action-card { display: block; padding: 20px; background: #f8f9fa; border-radius: 8px; text-decoration: none; color: inherit; transition: all 0.2s; border: 1px solid #eee; }
    .action-card:hover { background: #e9ecef; transform: translateY(-2px); }
    .action-card h4 { color: #007bff; margin-bottom: 5px; }
    .action-card p { font-size: 13px; color: #666; margin: 0; }
  `]
})
export class DashboardComponent implements OnInit {
  authService = inject(AuthService);
  brpService = inject(BrpService);
  catalogInfo = signal<CatalogUpdateResponse | null>(null);

  ngOnInit(): void {
    this.brpService.getCatalogoInfo().subscribe({
      next: (data) => this.catalogInfo.set(data),
      error: () => this.catalogInfo.set({ 
        ultimaActualizacion: new Date().toISOString(), 
        totalBrp: 0, 
        disponibles: 0,
        fechaFormateada: new Date().toLocaleString() 
      })
    });
  }
}
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrpService, BrpResponse } from '../../core/services/brp.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Catálogo de BRP</h2>

    <div class="filters">
      @if (puedeVerTodos()) {
        <button class="btn" [class.btn-primary]="filtro() === 'todos'" (click)="setFiltro('todos')">Todos</button>
      }
      <button class="btn" [class.btn-primary]="filtro() === 'disponibles'" (click)="setFiltro('disponibles')">Solo Disponibles</button>
    </div>

    @if (loading()) {
      <p>Cargando catálogo...</p>
    } @else {
      <div class="grid">
        @for (brp of brps(); track brp.codigo) {
          <div class="brp-card">
            <h3>{{ brp.codigo }}</h3>
            <span class="badge badge-{{ getEstadoClass(brp.estado) }}">{{ brp.estado }}</span>
            <div class="meta">
              <p><strong>Tipo:</strong> {{ brp.tipoBrp }}</p>
              <p><strong>Ubicación:</strong> {{ brp.municipio }}, {{ brp.departamento }}</p>
              <p><strong>Matrícula:</strong> {{ brp.matriculaInmobiliaria || brp.placa || 'N/A' }}</p>
            </div>
            <p class="descripcion">{{ brp.descripcion }}</p>
            @if (brp.precio) {
              <p class="precio">{{ brp.precio | currency:'COP':'symbol':'1.0-0' }}</p>
            } @else {
              <p class="precio sin-precio">Sin precio asignado</p>
            }
            <p class="ingesta">Ingesta: {{ brp.diaIngesta }}/{{ brp.mesIngesta }}/{{ brp.anioIngesta }}</p>
          </div>
        } @empty {
          <p>No hay BRP que mostrar.</p>
        }
      </div>
    }
  `,
  styles: [`
    .filters { margin-bottom: 20px; display: flex; gap: 10px; }
    .brp-card { position: relative; }
    .brp-card h3 { margin-bottom: 5px; }
    .meta { font-size: 13px; color: #666; margin: 10px 0; }
    .meta p { margin: 3px 0; }
    .descripcion { font-size: 14px; color: #555; margin: 10px 0; line-height: 1.4; }
    .ingesta { font-size: 12px; color: #999; margin-top: 10px; }
    .precio.sin-precio { color: #999; font-style: italic; }
  `]
})
export class CatalogoComponent implements OnInit {
  brpService = inject(BrpService);
  authService = inject(AuthService);

  brps = signal<BrpResponse[]>([]);
  loading = signal(true);
  filtro = signal<'todos' | 'disponibles'>('todos');

  ngOnInit(): void {
    // RADICADOR solo ve disponibles por defecto
    const esRadicador = this.authService.getUserRole() === 'RADICADOR';
    this.filtro.set(esRadicador ? 'disponibles' : 'todos');
    this.cargarBrps();
  }

  setFiltro(filtro: 'todos' | 'disponibles'): void {
    this.filtro.set(filtro);
    this.cargarBrps();
  }

  puedeVerTodos(): boolean {
    return this.authService.getUserRole() !== 'RADICADOR';
  }

  cargarBrps(): void {
    this.loading.set(true);
    const observable = this.filtro() === 'disponibles'
      ? this.brpService.getDisponibles()
      : this.brpService.getAll(this.authService.getUserRole());

    observable.subscribe({
      next: (data) => {
        this.brps.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  getEstadoClass(estado: string): string {
    const map: Record<string, string> = {
      'DISPONIBLE': 'disponible',
      'SIN_PRECIO': 'sin-precio',
      'PENDIENTE_TECNICO': 'pendiente',
      'EN_TRAMITE_DE_VENTA': 'disponible',
      'SANEAMIENTO': 'pendiente',
      'DADO_DE_BAJA': 'rechazado'
    };
    return map[estado] || 'disponible';
  }
}
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OfertaService, OfertaResponse } from '../../core/services/oferta.service';

@Component({
  selector: 'app-analisis',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Bandeja de Análisis</h2>

    @if (loading()) {
      <p>Cargando...</p>
    } @else {
      <div class="card">
        <h3>Ofertas en Proceso</h3>
        @if (ofertas().length === 0) {
          <p>No hay ofertas para analizar.</p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>BRP</th>
                <th>Valor</th>
                <th>Descuento</th>
                <th>Estado</th>
                <th>Radicado Por</th>
                <th>Adjuntos</th>
                <th>Fecha</th>
                <th>Motivo Devolución</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (oferta of ofertas(); track oferta.id) {
                <tr>
                  <td>{{ oferta.id }}</td>
                  <td>{{ oferta.brpCodigo }}</td>
                  <td>{{ oferta.valorTotal | currency:'COP':'symbol':'1.0-0' }}</td>
                  <td>{{ oferta.descuento | number:'1.2-2' }}%</td>
                  <td><span class="badge badge-{{ getEstadoClass(oferta.estado) }}">{{ oferta.estado }}</span></td>
                  <td>{{ oferta.radicadoPor }}</td>
                  <td>
                    @if (oferta.tieneAdjuntos) {
                      <span class="adjunto-badge" title="{{ oferta.totalAdjuntos }} documento(s)">📎 {{ oferta.totalAdjuntos }}</span>
                    } @else {
                      <span class="no-adjunto">-</span>
                    }
                  </td>
                  <td>{{ oferta.fechaCreacion | date:'short' }}</td>
                  <td>{{ oferta.motivoDevolucion || '-' }}</td>
                  <td class="actions">
                    @if (oferta.estado === 'RADICADO') {
                      <button class="btn btn-primary btn-sm" (click)="tomar(oferta)">Tomar</button>
                    } @else if (oferta.estado === 'EN_PROCESO') {
                      <button class="btn btn-primary btn-sm" (click)="escalar(oferta)">Escalar</button>
                    }
                    @if (oferta.estado !== 'RECHAZADO' && oferta.estado !== 'APROBADO') {
                      <button class="btn btn-danger btn-sm" (click)="rechazar(oferta)">Rechazar</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>

      @if (message()) {
        <div class="success-message">{{ message() }}</div>
      }
      @if (error()) {
        <div class="error-message">{{ error() }}</div>
      }
    }
  `,
  styles: [`
    .actions { display: flex; gap: 5px; }
    .btn-sm { padding: 5px 10px; font-size: 12px; }
    .badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
    .badge-RADICADO { background: #e3f2fd; color: #1565c0; }
    .badge-EN_PROCESO { background: #fff3e0; color: #e65100; }
    .badge-ESCALADO { background: #fce4ec; color: #c2185b; }
    .badge-APROBADO { background: #e8f5e9; color: #2e7d32; }
    .badge-RECHAZADO { background: #ffebee; color: #c62828; }
    .adjunto-badge { background: #e8f5e9; color: #2e7d32; padding: 2px 8px; border-radius: 12px; font-size: 11px; }
    .no-adjunto { color: #999; }
  `]
})
export class AnalisisComponent implements OnInit {
  ofertaService = inject(OfertaService);
  ofertas = signal<OfertaResponse[]>([]);
  loading = signal(true);
  message = signal('');
  error = signal('');

  ngOnInit(): void {
    this.cargarOfertas();
  }

  cargarOfertas(): void {
    this.ofertaService.getAll().subscribe({
      next: (data) => {
        // Solo mostrar estados activos (no finalizados)
        this.ofertas.set(data.filter(o => 
          o.estado === 'RADICADO' || 
          o.estado === 'EN_PROCESO' || 
          o.estado === 'ESCALADO'
        ));
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  getEstadoClass(estado: string): string {
    return estado;
  }

  tomar(oferta: OfertaResponse): void {
    if (confirm('¿Desea tomar esta oferta para análisis?')) {
      this.ofertaService.tomar(oferta.id).subscribe({
        next: () => {
          this.message.set(`Oferta ${oferta.id} tomada para análisis`);
          this.cargarOfertas();
        },
        error: (err) => this.error.set(err.error?.message || 'Error al tomar oferta')
      });
    }
  }

  escalar(oferta: OfertaResponse): void {
    this.ofertaService.escalar(oferta.id).subscribe({
      next: () => {
        this.message.set(`Oferta ${oferta.id} escalada exitosamente`);
        this.cargarOfertas();
      },
      error: (err) => this.error.set(err.error?.message || 'Error')
    });
  }

  rechazar(oferta: OfertaResponse): void {
    const motivo = prompt('Motivo del rechazo:');
    if (motivo === null) return; // Canceló
    
    this.ofertaService.rechazar(oferta.id, motivo).subscribe({
      next: () => {
        this.message.set(`Oferta ${oferta.id} rechazada`);
        this.cargarOfertas();
      },
      error: (err) => this.error.set(err.error?.message || 'Error')
    });
  }

  devolver(oferta: OfertaResponse): void {
    const motivo = prompt('Motivo de la devolución:');
    if (motivo === null) return; // Canceló
    
    this.ofertaService.devolver(oferta.id, motivo).subscribe({
      next: () => {
        this.message.set(`Oferta ${oferta.id} devuelta al analista`);
        this.cargarOfertas();
      },
      error: (err) => this.error.set(err.error?.message || 'Error al devolver')
    });
  }
}
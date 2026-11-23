import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OfertaService, OfertaResponse } from '../../core/services/oferta.service';

@Component({
  selector: 'app-aprobacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h2>Bandeja de Aprobación</h2>

    @if (loading()) {
      <p>Cargando...</p>
    } @else {
      <div class="card">
        <h3>Ofertas Escaladas</h3>
        @if (ofertas().length === 0) {
          <p>No hay ofertas para aprobar.</p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>BRP</th>
                <th>Valor</th>
                <th>Descuento</th>
                <th>Analista</th>
                <th>Adjuntos</th>
                <th>Fecha Escalamiento</th>
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
                  <td>{{ oferta.analista }}</td>
                  <td>
                    @if (oferta.tieneAdjuntos) {
                      <span class="adjunto-badge" title="{{ oferta.totalAdjuntos }} documento(s)">📎 {{ oferta.totalAdjuntos }}</span>
                    } @else {
                      <span class="no-adjunto">-</span>
                    }
                  </td>
                  <td>{{ oferta.fechaEscalamiento | date:'short' }}</td>
                  <td class="actions">
                    <button class="btn btn-success btn-sm" (click)="aprobar(oferta)">Aprobar</button>
                    <button class="btn btn-warning btn-sm" (click)="devolver(oferta)">Devolver</button>
                    <button class="btn btn-danger btn-sm" (click)="rechazar(oferta)">Rechazar</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>

      @if (selectedOferta()) {
        <div class="card mt-4">
          <h4>Detalles de Oferta #{{ selectedOferta()!.id }}</h4>
          <div class="details-grid">
            <div class="detail-item">
              <strong>BRP:</strong> {{ selectedOferta()!.brpCodigo }}
            </div>
            <div class="detail-item">
              <strong>Valor Total:</strong> {{ selectedOferta()!.valorTotal | currency:'COP':'symbol':'1.0-0' }}
            </div>
            <div class="detail-item">
              <strong>Descuento:</strong> {{ selectedOferta()!.descuento | number:'1.2-2' }}%
            </div>
            <div class="detail-item">
              <strong>Estado:</strong> {{ selectedOferta()!.estado }}
            </div>
          </div>

          <h5>Oferentes</h5>
          <table class="nested-table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Identificación</th>
                <th>% Participación</th>
                <th>Email</th>
              </tr>
            </thead>
            <tbody>
              @for (oferente of selectedOferta()!.oferentes; track oferente.id) {
                <tr>
                  <td>{{ oferente.nombre }}</td>
                  <td>{{ oferente.tipoIdentificacion }}-{{ oferente.numeroIdentificacion }}</td>
                  <td>{{ oferente.porcentajeParticipacion }}%</td>
                  <td>{{ oferente.email }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

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
    .mt-4 { margin-top: 1rem; }
    .details-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; margin-bottom: 1rem; }
    .nested-table { width: 100%; font-size: 14px; margin-top: 10px; }
    .nested-table th { background-color: #f5f5f5; }
    .adjunto-badge { background: #e8f5e9; color: #2e7d32; padding: 2px 8px; border-radius: 12px; font-size: 11px; }
    .no-adjunto { color: #999; }
  `]
})
export class AprobacionComponent implements OnInit {
  ofertaService = inject(OfertaService);
  ofertas = signal<OfertaResponse[]>([]);
  selectedOferta = signal<OfertaResponse | null>(null);
  loading = signal(true);
  message = signal('');
  error = signal('');

  ngOnInit(): void {
    this.cargarOfertas();
  }

  cargarOfertas(): void {
    this.ofertaService.getEscaladas().subscribe({
      next: (data) => {
        this.ofertas.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  verDetalles(oferta: OfertaResponse): void {
    this.selectedOferta.set(oferta);
  }

  aprobar(oferta: OfertaResponse): void {
    this.ofertaService.aprobar(oferta.id).subscribe({
      next: () => {
        this.message.set(`Oferta ${oferta.id} aprobada exitosamente`);
        this.selectedOferta.set(null);
        this.cargarOfertas();
      },
      error: (err) => this.error.set(err.error?.message || 'Error al aprobar')
    });
  }

  devolver(oferta: OfertaResponse): void {
    const motivo = prompt('Motivo de devolución:');
    if (motivo) {
      this.ofertaService.devolver(oferta.id, motivo).subscribe({
        next: () => {
          this.message.set(`Oferta ${oferta.id} devuelta al analista`);
          this.selectedOferta.set(null);
          this.cargarOfertas();
        },
        error: (err) => this.error.set(err.error?.message || 'Error al devolver')
      });
    }
  }

  rechazar(oferta: OfertaResponse): void {
    const motivo = prompt('Motivo de rechazo:');
    if (motivo) {
      this.ofertaService.rechazar(oferta.id, motivo).subscribe({
        next: () => {
          this.message.set(`Oferta ${oferta.id} rechazada`);
          this.selectedOferta.set(null);
          this.cargarOfertas();
        },
        error: (err) => this.error.set(err.error?.message || 'Error al rechazar')
      });
    }
  }
}
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OfertaService, OfertaResponse } from '../../core/services/oferta.service';

@Component({
  selector: 'app-seguimiento',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Seguimiento de Ofertas</h2>

    @if (loading()) {
      <p>Cargando ofertas...</p>
    } @else if (ofertas().length === 0) {
      <p>No hay ofertas registradas.</p>
    } @else {
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>BRP</th>
            <th>Estado</th>
            <th>Valor</th>
            <th>Descuento</th>
            <th>Radicado Por</th>
            <th>Analista</th>
            <th>Adjuntos</th>
            <th>Fecha</th>
          </tr>
        </thead>
        <tbody>
          @for (oferta of ofertas(); track oferta.id) {
            <tr (click)="verDetalles(oferta)" class="clickable-row">
              <td>{{ oferta.id }}</td>
              <td>{{ oferta.brpCodigo }}</td>
              <td>
                <span class="badge" [attr.data-status]="oferta.estado">
                  {{ oferta.estado }}
                </span>
              </td>
              <td>{{ oferta.valorTotal | currency:'COP':'symbol':'1.0-0' }}</td>
              <td>{{ oferta.descuento | number:'1.2-2' }}%</td>
              <td>{{ oferta.radicadoPor }}</td>
              <td>{{ oferta.analista || '-' }}</td>
              <td>
                @if (oferta.tieneAdjuntos) {
                  <span class="adjunto-badge" title="{{ oferta.totalAdjuntos }} documento(s)">
                    📎 {{ oferta.totalAdjuntos }}
                  </span>
                } @else {
                  <span class="no-adjunto">-</span>
                }
              </td>
              <td>{{ oferta.fechaCreacion | date:'short' }}</td>
            </tr>
          }
        </tbody>
      </table>
    }

    <!-- Modal: Detalles de Oferta -->
    @if (selectedOferta()) {
      <div class="modal-overlay" (click)="cerrarDetalles()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <h3>Detalles de Oferta #{{ selectedOferta()!.id }}</h3>
          <div class="details-grid">
            <div class="detail-item">
              <strong>BRP:</strong> {{ selectedOferta()!.brpCodigo }}
            </div>
            <div class="detail-item">
              <strong>Estado:</strong> {{ selectedOferta()!.estado }}
            </div>
            <div class="detail-item">
              <strong>Valor Total:</strong> {{ selectedOferta()!.valorTotal | currency:'COP':'symbol':'1.0-0' }}
            </div>
            <div class="detail-item">
              <strong>Descuento:</strong> {{ selectedOferta()!.descuento | number:'1.2-2' }}%
            </div>
            <div class="detail-item">
              <strong>Radicado Por:</strong> {{ selectedOferta()!.radicadoPor }}
            </div>
            <div class="detail-item">
              <strong>Analista:</strong> {{ selectedOferta()!.analista || '-' }}
            </div>
            <div class="detail-item">
              <strong>Fecha Creación:</strong> {{ selectedOferta()!.fechaCreacion | date:'medium' }}
            </div>
            @if (selectedOferta()!.fechaFinalizacion) {
              <div class="detail-item">
                <strong>Fecha Finalización:</strong> {{ selectedOferta()!.fechaFinalizacion | date:'medium' }}
              </div>
            }
            @if (selectedOferta()!.motivoDevolucion) {
              <div class="detail-item motivo-devolucion">
                <strong>Motivo Devolución:</strong> {{ selectedOferta()!.motivoDevolucion }}
              </div>
            }
          </div>

          <h5>Oferentes y Pagos</h5>
          @for (oferente of selectedOferta()!.oferentes; track oferente.id) {
            <div class="oferente-card">
              <strong>{{ oferente.nombre }}</strong> ({{ oferente.tipoIdentificacion }}-{{ oferente.numeroIdentificacion }})
              <br>
              <small>Participación: {{ oferente.porcentajeParticipacion }}%</small>
              
              @if (oferente.pagos.length > 0) {
                <table class="nested-table">
                  <thead>
                    <tr>
                      <th>Tipo</th>
                      <th>Fecha</th>
                      <th>Valor</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (pago of oferente.pagos; track pago.id) {
                      <tr>
                        <td>{{ pago.tipoPago }}</td>
                        <td>{{ pago.fecha | date:'shortDate' }}</td>
                        <td>{{ pago.valor | currency:'COP':'symbol':'1.0-0' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              }
            </div>
          }

          <button class="btn btn-secondary mt-4" (click)="cerrarDetalles()">Cerrar</button>
        </div>
      </div>
    }
  `,
  styles: [`
    .clickable-row { cursor: pointer; }
    .clickable-row:hover { background-color: #f5f5f5; }
    .badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 500; }
    .badge[data-status="APROBADO"] { background-color: #e8f5e9; color: #2e7d32; }
    .badge[data-status="RECHAZADO"] { background-color: #ffebee; color: #c62828; }
    .badge[data-status="RADICADO"], .badge[data-status="EN_PROCESO"] { background-color: #e3f2fd; color: #1565c0; }
    .badge[data-status="ESCALADO"] { background-color: #fff3e0; color: #e65100; }
    .adjunto-badge { background: #e8f5e9; color: #2e7d32; padding: 2px 8px; border-radius: 12px; font-size: 11px; }
    .no-adjunto { color: #999; }
    .details-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; margin: 10px 0; }
    .motivo-devolucion { grid-column: 1 / -1; background: #fff3e0; padding: 10px; border-radius: 4px; }
    .oferente-card { background: #f9f9f9; padding: 10px; margin: 10px 0; border-radius: 4px; border-left: 3px solid #1976d2; }
    .nested-table { width: 100%; font-size: 13px; margin-top: 5px; }
    .nested-table th { background-color: #f0f0f0; }
    .modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .modal-content { background: white; padding: 20px; border-radius: 8px; max-width: 700px; width: 90%; max-height: 80vh; overflow-y: auto; }
    .mt-4 { margin-top: 1rem; }
  `]
})
export class SeguimientoComponent implements OnInit {
  ofertaService = inject(OfertaService);

  ofertas = signal<OfertaResponse[]>([]);
  loading = signal(true);
  selectedOferta = signal<OfertaResponse | null>(null);

  ngOnInit(): void {
    this.cargarOfertas();
  }

  cargarOfertas(): void {
    this.ofertaService.getAll().subscribe({
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

  cerrarDetalles(): void {
    this.selectedOferta.set(null);
  }
}
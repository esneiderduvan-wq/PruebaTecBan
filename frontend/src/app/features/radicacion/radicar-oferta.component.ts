import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BrpService, BrpResponse } from '../../core/services/brp.service';
import { OfertaService, OfertaRequest, OferenteRequest, PagoRequest } from '../../core/services/oferta.service';
import { AdjuntoService } from '../../core/services/adjunto.service';

@Component({
  selector: 'app-radicar-oferta',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h2>Radicar Nueva Oferta</h2>

    <div class="card">
      <h3>Seleccionar BRP</h3>
      <div class="form-group">
        <label for="brpSelect">Código del BRP</label>
        <select id="brpSelect" [(ngModel)]="selectedBrpCodigo" (change)="onBrpSelect()">
          <option value="">Seleccione un BRP...</option>
          @for (brp of brpsDisponibles(); track brp.codigo) {
            <option [value]="brp.codigo">{{ brp.codigo }} - {{ brp.tipoBrp }} ({{ brp.municipio }})</option>
          }
        </select>
      </div>

      @if (selectedBrp()) {
        <div class="brp-details">
          <p><strong>Tipo:</strong> {{ selectedBrp()!.tipoBrp }}</p>
          <p><strong>Ubicación:</strong> {{ selectedBrp()!.municipio }}, {{ selectedBrp()!.departamento }}</p>
          <p><strong>Precio:</strong> {{ selectedBrp()!.precio | currency:'COP':'symbol':'1.0-0' }}</p>
          <p class="desc">{{ selectedBrp()!.descripcion }}</p>
        </div>
      }
    </div>

    @if (selectedBrp()) {
      <div class="card">
        <h3>Documentos Adjuntos</h3>
        <div class="file-upload">
          <input type="file" #fileInput (change)="onFileSelected($event)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png">
          @if (selectedFile()) {
            <span class="file-name">{{ selectedFile()!.name }}</span>
            <button class="btn btn-sm btn-secondary" (click)="clearFile()">Quitar</button>
          }
        </div>
        <p class="info">Formatos permitidos: PDF, DOC, DOCX, JPG, PNG (máx. 10MB)</p>
      </div>

      <p class="info"><strong>Nota:</strong> El porcentaje de participación se calcula automáticamente según el valor de los pagos.</p>
      
      @for (oferente of oferentes(); track $index) {
        <div class="oferente-card">
          <div class="oferente-header">
            <h4>Oferente {{ $index + 1 }}</h4>
            <span class="porcentaje-badge" [class.valid]="getPorcentaje(oferente) > 0" [class.empty]="getPorcentaje(oferente) === 0">
              {{ getPorcentaje(oferente) | number:'1.2-2' }}%
            </span>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Tipo ID</label>
              <select [(ngModel)]="oferente.tipoIdentificacion">
                <option value="CC">Cédula</option>
                <option value="NIT">NIT</option>
              </select>
            </div>
            <div class="form-group">
              <label>Número ID</label>
              <input type="text" [(ngModel)]="oferente.numeroIdentificacion" placeholder="12345678">
            </div>
            <div class="form-group">
              <label>Nombre/Razón Social</label>
              <input type="text" [(ngModel)]="oferente.nombre" placeholder="Juan Pérez">
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Email</label>
              <input type="email" [(ngModel)]="oferente.email" placeholder="juan@email.com">
            </div>
            <div class="form-group">
              <label>Teléfono</label>
              <input type="text" [(ngModel)]="oferente.telefono" placeholder="3001234567">
            </div>
            <div class="form-group">
              <label>Total Pagos Oferente</label>
              <input type="number" [value]="calcularPagosOferente(oferente)" 
                     (input)="updatePagosTotal(oferente, $event)" 
                     placeholder="Valor total del oferente">
            </div>
          </div>

          <h5>Pagos (distribución del valor total)</h5>
          @for (pago of oferente.pagos; track $index) {
            <div class="pago-row">
              <select [(ngModel)]="pago.tipoPago">
                <option value="CONTADO">Contado</option>
                <option value="PRESTAMO">Préstamo</option>
              </select>
              <input type="date" [(ngModel)]="pago.fecha">
              <input type="number" [(ngModel)]="pago.valor" placeholder="Valor" (input)="recalcularPorcentajes()">
              <input type="text" [(ngModel)]="pago.entidadFinanciadora" placeholder="Entidad (préstamo)">
              @if (oferente.pagos.length > 1) {
                <button class="btn btn-danger btn-sm" (click)="removePago($index, oferente)">X</button>
              }
            </div>
          }
          @if (oferente.pagos.length < 6) {
            <button class="btn btn-sm" (click)="addPago(oferente)">+ Agregar Pago</button>
          }
        </div>
      }

      @if (oferentes().length < 5) {
        <button class="btn" (click)="addOferente()">+ Agregar Oferente</button>
      }

      <div class="resumen">
        <p><strong>Total Pagos Oferta:</strong> {{ calcularTotalPagos() | currency:'COP':'symbol':'1.0-0' }}</p>
        <p class="porcentaje-info">
          <strong>Distribución de participación:</strong>
          @for (oferente of oferentes(); track $index) {
            <span class="porcentaje-item">
              Oferente {{ $index + 1 }}: {{ getPorcentaje(oferente) | number:'1.2-2' }}%
            </span>
          }
        </p>
      </div>

      @if (error()) {
        <div class="error-message">{{ error() }}</div>
      }
      @if (success()) {
        <div class="success-message">{{ success() }}</div>
      }

      <div class="actions">
        <button class="btn btn-success" (click)="radicar()" [disabled]="saving()">
          {{ saving() ? 'Radicando...' : 'Radicar Oferta' }}
        </button>
      </div>
    }
  `,
  styles: [`
    .brp-details { background: #f8f9fa; padding: 15px; border-radius: 4px; margin: 15px 0; }
    .brp-details .desc { font-style: italic; margin-top: 10px; color: #666; }
    .oferente-card { background: #f8f9fa; padding: 15px; border-radius: 4px; margin-bottom: 15px; }
    .oferente-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
    .oferente-header h4 { margin: 0; color: #007bff; }
    .porcentaje-badge { 
      background: #6c757d; color: white; padding: 5px 15px; border-radius: 20px; font-weight: bold;
    }
    .porcentaje-badge.valid { background: #28a745; }
    .porcentaje-badge.empty { background: #dc3545; }
    .form-row { display: flex; gap: 15px; margin-bottom: 10px; }
    .form-row .form-group { flex: 1; }
    .pago-row { display: flex; gap: 10px; margin-bottom: 5px; }
    .pago-row input, .pago-row select { flex: 1; }
    .btn-sm { padding: 5px 10px; font-size: 12px; margin-top: 5px; }
    .resumen { background: #e9ecef; padding: 15px; border-radius: 4px; margin: 15px 0; }
    .resumen p { margin: 5px 0; }
    .porcentaje-info { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px; }
    .porcentaje-item { background: #007bff; color: white; padding: 3px 10px; border-radius: 15px; font-size: 12px; }
    .actions { margin-top: 20px; }
    .info { background: #e7f3ff; padding: 10px; border-radius: 4px; font-size: 14px; color: #0056b3; }
    .file-upload { display: flex; align-items: center; gap: 10px; margin: 10px 0; }
    .file-name { font-weight: 500; color: #28a745; }
  `]
})
export class RadicarOfertaComponent {
  brpService = inject(BrpService);
  ofertaService = inject(OfertaService);
  adjuntoService = inject(AdjuntoService);
  router = inject(Router);

  brpsDisponibles = signal<BrpResponse[]>([]);
  selectedBrpCodigo = '';
  selectedBrp = signal<BrpResponse | null>(null);
  oferentes = signal<OferenteRequest[]>([]);
  saving = signal(false);
  error = signal('');
  success = signal('');
  selectedFile = signal<File | null>(null);
  uploadedFileName = signal<string | null>(null);

  // Cache para porcentajes calculados
  private porcentajesCache = new Map<OferenteRequest, number>();

  ngOnInit(): void {
    this.brpService.getDisponibles().subscribe(data => this.brpsDisponibles.set(data));
  }

  onBrpSelect(): void {
    const brp = this.brpsDisponibles().find(b => b.codigo === this.selectedBrpCodigo);
    this.selectedBrp.set(brp || null);
    this.oferentes.set([this.createOferenteVacio()]);
    this.porcentajesCache.clear();
  }

  addOferente(): void {
    this.oferentes.update(list => [...list, this.createOferenteVacio()]);
    this.recalcularPorcentajes();
  }

  createOferenteVacio(): OferenteRequest {
    return {
      tipoIdentificacion: 'CC',
      numeroIdentificacion: '',
      nombre: '',
      email: '',
      telefono: '',
      pagos: [{
        tipoPago: 'CONTADO',
        fecha: new Date().toISOString().split('T')[0],
        valor: 0
      }]
    };
  }

  addPago(oferente: OferenteRequest): void {
    if (oferente.pagos.length < 6) {
      oferente.pagos = [...oferente.pagos, {
        tipoPago: 'CONTADO',
        fecha: new Date().toISOString().split('T')[0],
        valor: 0
      }];
      this.recalcularPorcentajes();
    }
  }

  removePago(pagoIndex: number, oferente: OferenteRequest): void {
    oferente.pagos = oferente.pagos.filter((_, i) => i !== pagoIndex);
    this.recalcularPorcentajes();
  }

  updatePagosTotal(oferente: OferenteRequest, event: Event): void {
    const total = parseFloat((event.target as HTMLInputElement).value) || 0;
    if (oferente.pagos.length === 1) {
      oferente.pagos[0].valor = total;
    } else {
      // Distribuir el total entre los pagos existentes
      const count = oferente.pagos.length;
      const valorPorPago = count > 0 ? total / count : 0;
      oferente.pagos.forEach(p => p.valor = valorPorPago);
    }
    this.recalcularPorcentajes();
  }

  calcularPagosOferente(oferente: OferenteRequest): number {
    return oferente.pagos.reduce((sum, p) => sum + (p.valor || 0), 0);
  }

  calcularTotalPagos(): number {
    return this.oferentes().reduce((sumO, o) => 
      sumO + o.pagos.reduce((sumP, p) => sumP + (p.valor || 0), 0), 0);
  }

  recalcularPorcentajes(): void {
    const total = this.calcularTotalPagos();
    this.porcentajesCache.clear();
    
    if (total > 0) {
      this.oferentes().forEach(oferente => {
        const pagosOferente = this.calcularPagosOferente(oferente);
        const porcentaje = (pagosOferente / total) * 100;
        this.porcentajesCache.set(oferente, parseFloat(porcentaje.toFixed(2)));
      });
    }
  }

  getPorcentaje(oferente: OferenteRequest): number {
    return this.porcentajesCache.get(oferente) || 0;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile.set(input.files[0]);
    }
  }

  clearFile(): void {
    this.selectedFile.set(null);
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    if (fileInput) fileInput.value = '';
  }

  radicar(): void {
    this.error.set('');
    this.success.set('');

    if (!this.selectedBrp()) {
      this.error.set('Seleccione un BRP');
      return;
    }

    if (this.oferentes().length === 0) {
      this.error.set('Debe agregar al menos un oferente');
      return;
    }

    // Verificar que cada oferente tenga al menos un pago con valor > 0
    for (const oferente of this.oferentes()) {
      const total = this.calcularPagosOferente(oferente);
      if (total <= 0) {
        this.error.set('Cada oferente debe tener pagos con valor mayor a 0');
        return;
      }
    }

    const total = this.calcularTotalPagos();
    if (total <= 0) {
      this.error.set('El total de pagos debe ser mayor a 0');
      return;
    }

    this.saving.set(true);

    const request: OfertaRequest = {
      brpCodigo: this.selectedBrpCodigo,
      oferentes: this.oferentes()
    };

    this.ofertaService.radicar(request).subscribe({
      next: (response) => {
        // Subir archivo si hay uno seleccionado
        if (this.selectedFile()) {
          this.adjuntoService.uploadAdjunto(response.id, this.selectedFile()!, 'Documento oferta').subscribe({
            next: () => {
              this.uploadedFileName.set(this.selectedFile()!.name);
            },
            error: (err) => {
              console.warn('Error uploading file:', err);
            }
          });
        }
        this.success.set(`Oferta radicada exitosamente. ID: ${response.id}`);
        this.saving.set(false);
        setTimeout(() => this.router.navigate(['/seguimiento']), 2000);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Error al radicar oferta');
        this.saving.set(false);
      }
    });
  }
}
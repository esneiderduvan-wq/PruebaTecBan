import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface OfertaResponse {
  id: number;
  brpCodigo: string;
  brpDescripcion: string;
  valorTotal: number;
  descuento: number | null;
  estado: string;
  radicadoPor: string;
  analista: string | null;
  fechaCreacion: string;
  fechaModificacion: string | null;
  fechaEscalamiento: string | null;
  fechaDevolucion: string | null;
  motivoDevolucion: string | null;
  fechaFinalizacion: string | null;
  oferentes: OferenteResponse[];
  tieneAdjuntos: boolean;
  totalAdjuntos: number;
}

export interface OferenteResponse {
  id: number;
  tipoIdentificacion: string;
  numeroIdentificacion: string;
  nombre: string;
  porcentajeParticipacion: number;
  email: string | null;
  telefono: string | null;
  pagos: PagoResponse[];
}

export interface PagoResponse {
  id: number;
  tipoPago: string;
  fecha: string;
  valor: number;
  entidadFinanciadora: string | null;
}

export interface OfertaRequest {
  brpCodigo: string;
  oferentes: OferenteRequest[];
}

export interface OferenteRequest {
  tipoIdentificacion: string;
  numeroIdentificacion: string;
  nombre: string;
  porcentajeParticipacion?: number;
  email?: string;
  telefono?: string;
  pagos: PagoRequest[];
}

export interface PagoRequest {
  tipoPago: string;
  fecha: string;
  valor: number;
  entidadFinanciadora?: string;
}

@Injectable({
  providedIn: 'root'
})
export class OfertaService {
  private apiUrl = 'http://localhost:8080/api/ofertas';

  constructor(private http: HttpClient) {}

  getAll(): Observable<OfertaResponse[]> {
    return this.http.get<OfertaResponse[]>(this.apiUrl);
  }

  getById(id: number): Observable<OfertaResponse> {
    return this.http.get<OfertaResponse>(`${this.apiUrl}/${id}`);
  }

  getEscaladas(): Observable<OfertaResponse[]> {
    return this.http.get<OfertaResponse[]>(`${this.apiUrl}/escaladas`);
  }

  radicar(request: OfertaRequest): Observable<OfertaResponse> {
    return this.http.post<OfertaResponse>(this.apiUrl, request);
  }

  tomar(id: number): Observable<OfertaResponse> {
    return this.http.post<OfertaResponse>(`${this.apiUrl}/${id}/tomar`, {});
  }

  escalar(id: number): Observable<OfertaResponse> {
    return this.http.post<OfertaResponse>(`${this.apiUrl}/${id}/escalar`, {});
  }

  aprobar(id: number): Observable<OfertaResponse> {
    return this.http.post<OfertaResponse>(`${this.apiUrl}/${id}/aprobar`, {});
  }

  rechazar(id: number, motivo?: string): Observable<OfertaResponse> {
    return this.http.post<OfertaResponse>(`${this.apiUrl}/${id}/rechazar`, { motivo });
  }

  devolver(id: number, motivo?: string): Observable<OfertaResponse> {
    return this.http.post<OfertaResponse>(`${this.apiUrl}/${id}/devolver`, { motivo });
  }
}
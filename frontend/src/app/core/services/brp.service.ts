import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BrpResponse {
  codigo: string;
  estado: string;
  tipoBrp: string;
  matriculaInmobiliaria: string | null;
  placa: string | null;
  departamento: string;
  municipio: string;
  descripcion: string;
  precio: number | null;
  fechaAsignacionPrecio: string | null;
  anioIngesta: number;
  mesIngesta: number;
  diaIngesta: number;
}

export interface CatalogUpdateResponse {
  ultimaActualizacion: string;
  fechaFormateada: string;
  totalBrp: number;
  disponibles: number;
}

@Injectable({
  providedIn: 'root'
})
export class BrpService {
  private apiUrl = 'http://localhost:8080/api/brp';

  constructor(private http: HttpClient) {}

  getAll(rol: string): Observable<BrpResponse[]> {
    const headers = new HttpHeaders().set('X-Rol', rol);
    return this.http.get<BrpResponse[]>(this.apiUrl, { headers });
  }

  getDisponibles(): Observable<BrpResponse[]> {
    return this.http.get<BrpResponse[]>(`${this.apiUrl}/disponibles`);
  }

  getByCodigo(codigo: string): Observable<BrpResponse> {
    return this.http.get<BrpResponse>(`${this.apiUrl}/${codigo}`);
  }

  getCatalogoInfo(): Observable<CatalogUpdateResponse> {
    return this.http.get<CatalogUpdateResponse>(`${this.apiUrl}/catalogo/ultima-actualizacion`);
  }
}
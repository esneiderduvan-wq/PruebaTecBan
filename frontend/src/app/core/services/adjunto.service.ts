import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AdjuntoResponse {
  id: number;
  nombreArchivo: string;
  descripcion: string | null;
  rutaLocal: string;
  fechaCarga: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdjuntoService {
  private apiUrl = 'http://localhost:8080/api/adjuntos';

  constructor(private http: HttpClient) {}

  uploadAdjunto(ofertaId: number, file: File, nombre: string): Observable<AdjuntoResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('nombre', nombre);
    return this.http.post<AdjuntoResponse>(`${this.apiUrl}/upload/${ofertaId}`, formData);
  }

  getByOferta(ofertaId: number): Observable<AdjuntoResponse[]> {
    return this.http.get<AdjuntoResponse[]>(`${this.apiUrl}/oferta/${ofertaId}`);
  }

  download(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${id}`, { responseType: 'blob' });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
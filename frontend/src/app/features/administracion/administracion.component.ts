import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService, UsuarioResponse, UsuarioRequest } from '../../core/services/usuario.service';

@Component({
  selector: 'app-administracion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h2>Administración de Usuarios</h2>

    <div class="toolbar">
      <button class="btn btn-primary" (click)="abrirModalCrear()">
        + Nuevo Usuario
      </button>
      <input 
        type="text" 
        [ngModel]="searchTerm" 
        (ngModelChange)="onSearchChange($event)"
        placeholder="Buscar usuario..."
        class="search-input">
    </div>

    @if (loading()) {
      <p>Cargando usuarios...</p>
    } @else {
      <table>
        <thead>
          <tr>
            <th>Username</th>
            <th>Nombre</th>
            <th>Email</th>
            <th>Rol</th>
            <th>Módulos (autoasignados)</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          @for (usuario of filteredUsuarios(); track usuario.id) {
            <tr>
              <td>{{ usuario.username }}</td>
              <td>{{ usuario.nombre }}</td>
              <td>{{ usuario.email }}</td>
              <td>
                <span class="badge" [attr.data-rol]="usuario.rol">{{ usuario.rol }}</span>
              </td>
              <td>{{ usuario.modulos.join(', ') || '-' }}</td>
              <td>
                <span [class]="usuario.activo ? 'status-active' : 'status-inactive'">
                  {{ usuario.activo ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td class="actions">
                <button class="btn btn-sm btn-secondary" (click)="abrirModalEditar(usuario)">Editar</button>
                <button class="btn btn-sm" 
                  [class.btn-danger]="usuario.activo" 
                  [class.btn-success]="!usuario.activo"
                  (click)="toggleEstado(usuario)">
                  {{ usuario.activo ? 'Desactivar' : 'Activar' }}
                </button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="7" class="text-center">No hay usuarios registrados.</td>
            </tr>
          }
        </tbody>
      </table>
    }

    <!-- Modal Usuario -->
    @if (showModal()) {
      <div class="modal-overlay" (click)="cerrarModal()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <h3>{{ editando() ? 'Editar' : 'Crear' }} Usuario</h3>
          <p class="info">Los módulos se asignan automáticamente según el rol.</p>
          
          <form (ngSubmit)="guardar()">
            <div class="form-group">
              <label for="username">Username:</label>
              <input type="text" id="username" [(ngModel)]="formData.username" name="username" required [disabled]="editando()">
            </div>

            @if (!editando()) {
              <div class="form-group">
                <label for="password">Password:</label>
                <input type="password" id="password" [(ngModel)]="formData.password" name="password" required>
              </div>
            }

            <div class="form-group">
              <label for="nombre">Nombre Completo:</label>
              <input type="text" id="nombre" [(ngModel)]="formData.nombre" name="nombre" required>
            </div>

            <div class="form-group">
              <label for="email">Email:</label>
              <input type="email" id="email" [(ngModel)]="formData.email" name="email" required>
            </div>

            <div class="form-group">
              <label for="rol">Rol:</label>
              <select id="rol" [(ngModel)]="formData.rol" name="rol" required>
                <option value="ADMINISTRADOR">ADMINISTRADOR - Todos los permisos</option>
                <option value="RADICADOR">RADICADOR - Radicar ofertas</option>
                <option value="ANALISTA">ANALISTA - Analizar ofertas</option>
                <option value="JEFE">JEFE - Aprobar ofertas</option>
              </select>
            </div>

            <div class="modulos-preview" *ngIf="formData.rol">
              <label>Módulos asignados:</label>
              <div class="badges">
                @for (modulo of getModulosPorRol(formData.rol); track modulo) {
                  <span class="modulo-badge">{{ modulo }}</span>
                }
              </div>
            </div>

            <div class="form-actions">
              <button type="button" class="btn btn-secondary" (click)="cerrarModal()">Cancelar</button>
              <button type="submit" class="btn btn-primary" [disabled]="saving()">
                {{ saving() ? 'Guardando...' : 'Guardar' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (message()) {
      <div class="success-message">{{ message() }}</div>
    }
    @if (error()) {
      <div class="error-message">{{ error() }}</div>
    }
  `,
  styles: [`
    .toolbar { display: flex; gap: 10px; margin-bottom: 1rem; align-items: center; }
    .search-input { padding: 8px; border: 1px solid #ddd; border-radius: 4px; min-width: 250px; }
    .actions { display: flex; gap: 5px; }
    .badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
    .badge[data-rol="ADMINISTRADOR"] { background: #e8eaf6; color: #3949ab; }
    .badge[data-rol="RADICADOR"] { background: #e3f2fd; color: #1565c0; }
    .badge[data-rol="ANALISTA"] { background: #fff3e0; color: #e65100; }
    .badge[data-rol="JEFE"] { background: #e8f5e9; color: #2e7d32; }
    .status-active { color: #2e7d32; font-weight: 500; }
    .status-inactive { color: #c62828; font-weight: 500; }
    .text-center { text-align: center; color: #666; }
    .modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .modal-content { background: white; padding: 20px; border-radius: 8px; max-width: 500px; width: 90%; max-height: 80vh; overflow-y: auto; }
    .form-group { margin-bottom: 15px; }
    .form-group label { display: block; margin-bottom: 5px; font-weight: 500; }
    .form-group input, .form-group select { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
    .modulos-preview { background: #f8f9fa; padding: 12px; border-radius: 4px; margin-bottom: 15px; }
    .modulos-preview label { display: block; margin-bottom: 8px; font-weight: 500; font-size: 12px; color: #666; }
    .badges { display: flex; flex-wrap: wrap; gap: 5px; }
    .modulo-badge { background: #007bff; color: white; padding: 3px 10px; border-radius: 15px; font-size: 12px; }
    .form-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }
    .btn-sm { padding: 5px 10px; font-size: 12px; }
    .info { background: #e7f3ff; padding: 10px; border-radius: 4px; font-size: 13px; color: #0056b3; margin-bottom: 15px; }
  `]
})
export class AdministracionComponent implements OnInit {
  private usuarioService = inject(UsuarioService);

  // Data
  usuarios = signal<UsuarioResponse[]>([]);
  filteredUsuarios = signal<UsuarioResponse[]>([]);
  loading = signal(true);
  saving = signal(false);

  // Modal
  showModal = signal(false);
  editando = signal(false);
  selectedUsuarioId = signal<number | null>(null);

  // Form
  formData = this.getEmptyForm();

  // Search
  searchTerm = '';

  // Messages
  message = signal('');
  error = signal('');

  // Módulos por rol (debe coincidir con UsuarioService.java)
  modulosPorRol: Record<string, string[]> = {
    ADMINISTRADOR: ['CATALOGO', 'SEGUIMIENTO', 'ADMINISTRACION'],
    RADICADOR: ['RADICACION', 'SEGUIMIENTO', 'CATALOGO'],
    ANALISTA: ['ANALISIS', 'SEGUIMIENTO', 'CATALOGO'],
    JEFE: ['APROBACION', 'SEGUIMIENTO', 'CATALOGO']
  };

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  getEmptyForm(): any {
    return {
      username: '',
      password: '',
      nombre: '',
      email: '',
      rol: 'RADICADOR'
    };
  }

  getModulosPorRol(rol: string): string[] {
    return this.modulosPorRol[rol] || [];
  }

  cargarUsuarios(): void {
    this.usuarioService.getAll().subscribe({
      next: (data) => {
        this.usuarios.set(data);
        this.filterUsers();
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onSearchChange(term: string): void {
    this.searchTerm = term;
    this.filterUsers();
  }

  filterUsers(): void {
    const usuarios = this.usuarios();
    if (!this.searchTerm.trim()) {
      this.filteredUsuarios.set(usuarios);
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredUsuarios.set(
        usuarios.filter(u => 
          u.username.toLowerCase().includes(term) ||
          u.nombre.toLowerCase().includes(term) ||
          u.email.toLowerCase().includes(term)
        )
      );
    }
  }

  abrirModalCrear(): void {
    this.editando.set(false);
    this.formData = this.getEmptyForm();
    this.showModal.set(true);
  }

  abrirModalEditar(usuario: UsuarioResponse): void {
    this.editando.set(true);
    this.selectedUsuarioId.set(usuario.id);
    this.formData = {
      username: usuario.username,
      nombre: usuario.nombre,
      email: usuario.email,
      rol: usuario.rol,
      password: ''
    };
    this.showModal.set(true);
  }

  cerrarModal(): void {
    this.showModal.set(false);
    this.editando.set(false);
    this.selectedUsuarioId.set(null);
    this.formData = this.getEmptyForm();
  }

  guardar(): void {
    this.saving.set(true);
    this.error.set('');

    // NO enviar módulos - se asignan automáticamente por rol
    const request = {
      username: this.formData.username,
      nombre: this.formData.nombre,
      email: this.formData.email,
      rol: this.formData.rol
    };
    
    // Agregar password solo en creación
    if (!this.editando() && this.formData.password) {
      (request as any).password = this.formData.password;
    }

    const selectedId = this.selectedUsuarioId();
    const operation = this.editando() && selectedId
      ? this.usuarioService.update(selectedId, request)
      : this.usuarioService.create(request);

    operation.subscribe({
      next: () => {
        this.message.set(`Usuario ${this.editando() ? 'actualizado' : 'creado'} exitosamente`);
        this.saving.set(false);
        this.cerrarModal();
        this.cargarUsuarios();
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Error al guardar usuario');
        this.saving.set(false);
      }
    });
  }

  toggleEstado(usuario: UsuarioResponse): void {
    if (confirm(`¿Está seguro de ${usuario.activo ? 'desactivar' : 'activar'} al usuario ${usuario.username}?`)) {
      const operation = usuario.activo 
        ? this.usuarioService.deactivate(usuario.id)
        : this.usuarioService.activate(usuario.id);

      operation.subscribe({
        next: () => {
          this.message.set(`Usuario ${usuario.username} ${usuario.activo ? 'desactivado' : 'activado'}`);
          this.cargarUsuarios();
        },
        error: (err) => this.error.set(err.error?.message || 'Error al cambiar estado')
      });
    }
  }
}
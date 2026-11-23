import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogo',
    loadComponent: () => import('./features/catalogo/catalogo.component').then(m => m.CatalogoComponent),
    canActivate: [authGuard]
  },
  {
    path: 'radicar',
    loadComponent: () => import('./features/radicacion/radicar-oferta.component').then(m => m.RadicarOfertaComponent),
    canActivate: [authGuard],
    data: { roles: ['RADICADOR'] }
  },
  {
    path: 'analisis',
    loadComponent: () => import('./features/analisis/analisis.component').then(m => m.AnalisisComponent),
    canActivate: [authGuard],
    data: { roles: ['ANALISTA'] }
  },
  {
    path: 'aprobacion',
    loadComponent: () => import('./features/aprobacion/aprobacion.component').then(m => m.AprobacionComponent),
    canActivate: [authGuard],
    data: { roles: ['JEFE'] }
  },
  {
    path: 'seguimiento',
    loadComponent: () => import('./features/seguimiento/seguimiento.component').then(m => m.SeguimientoComponent),
    canActivate: [authGuard]
  },
  {
    path: 'administracion',
    loadComponent: () => import('./features/administracion/administracion.component').then(m => m.AdministracionComponent),
    canActivate: [authGuard],
    data: { roles: ['ADMINISTRADOR'] }
  },
  { path: '**', redirectTo: '/dashboard' }
];
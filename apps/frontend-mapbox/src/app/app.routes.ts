import { Route } from '@angular/router';
import { AuthCallbackComponent } from './core/auth/auth-callback.component';
import { MapViewComponent } from './features/map/map-view.component';
import { inject } from '@angular/core';
import { AuthService } from './core/auth/auth.service';
import { Router } from '@angular/router';

const authGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.state().authenticated) return true;
  auth.login();
  return router.createUrlTree(['/']);
};

export const appRoutes: Route[] = [
  { path: 'auth/callback', component: AuthCallbackComponent },
  { path: 'map', component: MapViewComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/map', pathMatch: 'full' },
  { path: '**', redirectTo: '/map' },
];

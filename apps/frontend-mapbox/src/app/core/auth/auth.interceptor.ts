import { inject } from '@angular/core';
import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
} from '@angular/common/http';
import { AuthService } from '../auth/auth.service';
import { environment } from '../../../environments/environment';

/**
 * Attaches JWT Bearer token to requests targeting our own API.
 * Does NOT attach the token to third-party requests (Mapbox tiles etc).
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  if (token && req.url.startsWith(environment.apiBaseUrl)) {
    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
    return next(cloned);
  }

  return next(req);
};

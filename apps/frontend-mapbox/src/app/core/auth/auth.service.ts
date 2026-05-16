import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface OidcTokenResponse {
  access_token: string;
  refresh_token?: string;
  expires_in: number;
  token_type: string;
}

export interface AuthState {
  accessToken: string | null;
  expiresAt: number | null;
  authenticated: boolean;
}

const TOKEN_KEY = 'tmv_access_token';
const EXPIRES_KEY = 'tmv_token_expires';

/**
 * Minimal OIDC / OAuth2 client that works with any standards-compliant provider.
 * For local dev, a Keycloak dev-mode server is the recommended provider.
 *
 * Flow: redirect → authorization code → token exchange
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly state = signal<AuthState>(this.loadFromStorage());

  private loadFromStorage(): AuthState {
    const token = sessionStorage.getItem(TOKEN_KEY);
    const expiresAt = Number(sessionStorage.getItem(EXPIRES_KEY) ?? '0');
    if (token && expiresAt > Date.now()) {
      return { accessToken: token, expiresAt, authenticated: true };
    }
    return { accessToken: null, expiresAt: null, authenticated: false };
  }

  /** Redirect browser to OIDC provider authorization endpoint */
  login(): void {
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: environment.oidcClientId,
      redirect_uri: `${window.location.origin}/auth/callback`,
      scope: 'openid profile email',
    });
    window.location.href = `${environment.oidcAuthority}/protocol/openid-connect/auth?${params}`;
  }

  /**
   * Exchange authorization code for tokens.
   * Called from the /auth/callback route.
   */
  handleCallback(code: string): void {
    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: environment.oidcClientId,
      redirect_uri: `${window.location.origin}/auth/callback`,
      code,
    });

    this.http
      .post<OidcTokenResponse>(
        `${environment.oidcAuthority}/protocol/openid-connect/token`,
        body.toString(),
        { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
      )
      .subscribe({
        next: (resp) => {
          const expiresAt = Date.now() + resp.expires_in * 1000;
          sessionStorage.setItem(TOKEN_KEY, resp.access_token);
          sessionStorage.setItem(EXPIRES_KEY, String(expiresAt));
          this.state.set({
            accessToken: resp.access_token,
            expiresAt,
            authenticated: true,
          });
        },
        error: (err) => console.error('Token exchange failed', err),
      });
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(EXPIRES_KEY);
    this.state.set({ accessToken: null, expiresAt: null, authenticated: false });
    const params = new URLSearchParams({
      client_id: environment.oidcClientId,
      post_logout_redirect_uri: window.location.origin,
    });
    window.location.href = `${environment.oidcAuthority}/protocol/openid-connect/logout?${params}`;
  }

  getToken(): string | null {
    return this.state().accessToken;
  }
}

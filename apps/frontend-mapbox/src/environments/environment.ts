export const environment = {
  production: false,
  /** Backend API base URL - override with your actual backend address */
  apiBaseUrl: 'http://localhost:8080',
  /** SSE stream endpoint */
  sseUrl: 'http://localhost:8080/api/telemetry/stream',
  /**
   * Mapbox public token.
   * Replace with your own token from https://account.mapbox.com
   * For local dev, you can also set MAPBOX_TOKEN in a .env file.
   */
  mapboxToken: '',
  /** OAuth2 / OIDC authority (local Keycloak in dev mode) */
  oidcAuthority: 'http://localhost:8180/realms/telemetry',
  oidcClientId: 'telemetry-frontend',
};

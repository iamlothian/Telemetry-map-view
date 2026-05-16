describe('Telemetry Map View — E2E', () => {
  /**
   * Bypass OIDC redirect in tests by pre-populating a token in sessionStorage.
   * The backend test profile accepts tokens signed with a known test key.
   * Pass the test JWT via Cypress env var TEST_JWT.
   */
  beforeEach(() => {
    cy.window().then((win) => {
      const future = Date.now() + 60 * 60 * 1000;
      win.sessionStorage.setItem(
        'tmv_access_token',
        Cypress.env('TEST_JWT') ?? 'cy-test-token'
      );
      win.sessionStorage.setItem('tmv_token_expires', String(future));
    });
  });

  it('should load the application shell', () => {
    cy.visit('/');
    cy.get('app-root').should('exist');
  });

  it('should redirect to the map route', () => {
    cy.visit('/');
    cy.url().should('include', '/map');
  });

  it('should render the Mapbox container', () => {
    cy.visit('/map');
    cy.get('.map-container').should('exist');
    // Mapbox GL canvas is created asynchronously
    cy.get('canvas.mapboxgl-canvas', { timeout: 10_000 }).should('exist');
  });

  it('should display the feed status panel', () => {
    cy.visit('/map');
    cy.get('app-feed-status-panel').should('exist');
  });

  it('should show disconnected status when SSE is unavailable', () => {
    cy.intercept('GET', '/api/telemetry/stream', { forceNetworkError: true }).as(
      'sseStream'
    );
    cy.visit('/map');
    cy.get('.stream-status.disconnected', { timeout: 10_000 }).should(
      'be.visible'
    );
  });

  it('should render train markers when telemetry events arrive via SSE', () => {
    const mockPosition = {
      trainId: 'amtrak:test-1',
      displayName: 'Test Express',
      coordinates: [-87.65, 41.85],
      speedKmh: 100,
      bearing: 90,
      reportedAtMs: Date.now() - 1000,
      receivedAtMs: Date.now(),
      vehicleType: 'passenger',
      feedId: 'amtrak',
      operatorCode: 'AMTK',
      tripId: '1',
      routeShortName: 'Test Line',
      destination: 'Chicago',
    };

    cy.intercept('GET', '/api/telemetry/stream', (req) => {
      req.reply({
        statusCode: 200,
        headers: { 'Content-Type': 'text/event-stream' },
        body: `event: position_update\ndata: ${JSON.stringify(mockPosition)}\n\n`,
      });
    }).as('sseStream');

    cy.visit('/map');
    cy.wait('@sseStream');

    cy.get('.train-marker[data-train-id="amtrak:test-1"]', { timeout: 8000 }).should(
      'exist'
    );
  });
});


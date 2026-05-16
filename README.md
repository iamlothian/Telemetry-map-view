# Telemetry Map View

Real-time geospatial visualization of North American train positions. An **Angular + Mapbox** frontend streams live train positions from a **Spring Boot WebFlux** backend over Server-Sent Events (SSE). The backend ingests multiple public/free GTFS-RT and proprietary feeds.

---

## Architecture

```
telemetry-map-view/                     ← Nx monorepo root
├── apps/
│   ├── frontend-mapbox/                ← Angular 17+ SPA (Mapbox GL, Vitest)
│   ├── frontend-mapbox-e2e/            ← Cypress E2E tests
│   └── backend-api/                    ← Spring Boot 3 / WebFlux (Maven)
├── libs/
│   └── shared-models/                  ← Shared TypeScript telemetry types
└── docs/                               ← Architecture & operational guides
```

**Data flow:**
```
Public Feeds (GTFS-RT / JSON)
      │
      ▼
FeedAdapter (per feed) ──► TelemetryIngestionService (Reactor Flux)
                                  │
                          SSE endpoint /api/telemetry/stream
                                  │
                          Angular TelemetryStreamService
                                  │
                          Mapbox train markers (live update)
```

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Node.js | ≥ 20 LTS | `node -v` |
| npm | ≥ 10 | bundled with Node |
| Java | 21 | JDK (not JRE) |
| Docker | any | for local Keycloak identity provider |

---

## Quick Start (Local Development)

### 1. Install dependencies

```bash
npm install
```

### 2. Start the local identity provider (Keycloak)

```bash
docker run --name keycloak-dev \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev
```

Then open http://localhost:8180 and:
- Create realm **`telemetry`**
- Create client **`telemetry-frontend`** (type: public, valid redirect URI: `http://localhost:4200/*`)
- Create a test user with a known password

> A Keycloak realm export (`docs/keycloak-realm.json`) will be added in the next iteration to automate this.

### 3. Configure your Mapbox token

Edit [`apps/frontend-mapbox/src/environments/environment.ts`](apps/frontend-mapbox/src/environments/environment.ts):

```ts
mapboxToken: 'pk.eyJ1IjoiWU9VUl9VU0VSTkFNRSIsImEiOiI...', // your token
```

Get a free token at https://account.mapbox.com

### 4. Start the backend

```bash
cd apps/backend-api
./mvnw spring-boot:run
# Windows: .\mvnw.cmd spring-boot:run
```

Backend starts at http://localhost:8080  
SSE stream: `GET http://localhost:8080/api/telemetry/stream` (requires JWT Bearer token)  
Feed status: `GET http://localhost:8080/api/feeds/status` (public)

### 5. Start the frontend

```bash
npm start
# or: node .nx/nxw.js run frontend-mapbox:serve
```

Frontend starts at http://localhost:4200.

---

## Running Tests

### Frontend unit tests (Vitest)

```bash
node .nx/nxw.js run frontend-mapbox:test
```

### Backend unit tests (Maven Surefire)

```bash
cd apps/backend-api && ./mvnw test
```

### E2E tests (Cypress)

```bash
node .nx/nxw.js e2e frontend-mapbox-e2e
```

---

## Nx Commands

```bash
node .nx/nxw.js show projects
node .nx/nxw.js graph
node .nx/nxw.js affected --target=test
node .nx/nxw.js run-many --target=lint --all
```

---

## Feed Support

| Feed | Operator | Format | Credentials | Polling |
|------|----------|--------|-------------|---------|
| Amtrak VehiclePositions | AMTK (USA) | GTFS-RT | None | 30s |
| Metrolinx / GO Transit | GOTO (CAN) | GTFS-RT | None | 15s |
| VIA Rail | VIAR (CAN) | JSON | None | 30s |

### Adding a new GTFS-RT feed

1. Extend `AbstractGtfsRtFeedAdapter` in `apps/backend-api/src/main/java/com/telemetry/feed/adapters/`
2. Annotate with `@Component` — Spring auto-discovers it

### Adding a JSON feed

Implement `FeedAdapter` directly (see `ViaRailFeedAdapter` as a reference).

---

## Security

- All SSE/REST endpoints require a valid JWT Bearer token (except `/actuator/health` and `/api/feeds/status`)
- JWT validation via OIDC JWKS endpoint (configured in `application.properties`)
- Frontend attaches tokens only to requests targeting `apiBaseUrl` via `authInterceptor`
- Tokens stored in `sessionStorage` (session-scoped, not persistent)

---

## MVP Limitations

- **No persistence** — position history is in-memory; restart clears state
- **Single backend instance** — for >10k connections add Redis pub/sub
- **Mapbox token** — in `environment.ts`; use a secret manager for production

---

## Roadmap

- [ ] PostgreSQL + PostGIS for historical telemetry
- [ ] Additional feeds: MBTA, Caltrain, 511 SF Bay GTFS-RT
- [ ] Redis pub/sub for horizontal SSE scaling
- [ ] Docker Compose for one-command local start
- [ ] Keycloak realm auto-import script

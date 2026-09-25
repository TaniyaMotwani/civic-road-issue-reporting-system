# Roadwatch — Civic Road Issue Reporting System

Roadwatch is a small, modular civic platform for reporting road problems, grouping reports that appear to describe the same physical issue, and tracking progress. It is a student/portfolio project, not a government complaint service. It does not send reports to government systems.

## What is included

- React + Vite citizen and admin interface, served by the Java app in a single deployment.
- Spring Boot modular monolith and PostgreSQL persistence. Flyway creates the tables when the app starts.
- Citizen account registration/sign-in and a separately configured admin account. Passwords are BCrypt hashes; browser sessions use HttpOnly cookies and CSRF protection.
- Road report form with JPEG/PNG validation, an explicit browser location request, and a visible location preview.
- Reverse geocoding through OpenStreetMap Nominatim, with a per-process result cache and at most one upstream request per second.
- Local Ollama vision integration as optional decision support. Without it, reports are marked **Needs review** and are still usable.
- Duplicate grouping by issue type and distance (within 40 metres, for groups created in the previous 180 days). This is a practical starting heuristic, not a claim that two reports are definitely the same issue.
- Public area/type/status issue browsing, a citizen “My reports” view, and an admin dashboard with status history and AI review controls.
- Local disk image storage for development, with optional Supabase Storage for persistent hosted images.

This starter deliberately has no heat map, government integration, paid AI API, or complex GIS. The area → issue → reports drill-down uses a simple list and OpenStreetMap links.

## Run the complete app locally with Docker

### 1. Install tools

Install Docker Desktop and start it. Docker Compose builds the React frontend and Spring Boot backend, then starts the app and PostgreSQL.

### 2. Create your local settings file

From the project root, copy `.env.example` to `.env`. Fill in a unique database password, an admin email, and a different admin password of at least 12 characters. Put a contact address in `GEOCODING_USER_AGENT`. `.env` is ignored by Git and should stay on your machine.

### 3. Start Roadwatch

```sh
docker compose up --build
```

Open [http://localhost:8080](http://localhost:8080). Check the backend at [http://localhost:8080/api/health](http://localhost:8080/api/health). Stop the app with `Ctrl+C`; start it later with `docker compose up`.

The first run downloads the JavaScript, Java, and PostgreSQL build/runtime packages. Report photos and the database are kept in local Docker volumes. For a clean local restart that preserves data, use `docker compose down` (do not add `-v`).

### Admin account

The admin account is created on startup from `ADMIN_EMAIL` and `ADMIN_PASSWORD` in `.env`. Use a different password from the database password. Do not put either secret in source code or commit `.env`.

### Run services separately for development

Prerequisites: Node.js 22.13+, pnpm 11, Java 17+, and Maven 3.6.3+.

Start only PostgreSQL with `docker compose up -d database`. In one terminal, set `DATABASE_URL=jdbc:postgresql://localhost:5432/roadwatch`, `DATABASE_USER=roadwatch`, `DATABASE_PASSWORD` to the same local value as in `.env`, and `ADMIN_EMAIL`/`ADMIN_PASSWORD`, then run:

```sh
cd backend
mvn spring-boot:run
```

In another terminal:

```sh
cd frontend
pnpm install --frozen-lockfile
pnpm run dev
```

Open the Vite URL printed in the terminal (normally `http://localhost:5173`). Vite forwards `/api` to Spring Boot. On PowerShell, set a variable for the current terminal with `$env:DATABASE_PASSWORD = "your-local-value"` (repeat for the other variables); on macOS/Linux use `export DATABASE_PASSWORD="your-local-value"`.

## Database: what the tables represent

PostgreSQL stores structured report data, not photo bytes:

- `app_users` holds email, password hash, role, and account creation time.
- `issue_groups` represents one physical road issue with its public code, location, type, severity, and current status.
- `reports` holds each citizen’s separate photo/report and links it to its user and shared issue group.
- `status_history` records administrator status changes and notes.

The `backend/src/main/resources/db/migration` SQL files create and evolve this schema automatically using Flyway. Do not manually recreate these tables in a second place.

## Optional services

### Supabase PostgreSQL and image storage

Supabase has a Free plan that includes (as of September 2026) 2 projects, 500 MB database per project, 1 GB file storage, and 5 GB egress. Free projects can pause after 7 days of low activity; backups are not included. Keep uploads and downloads within the quota, export your database for backups, and check the current plan before using it. Staying within these limits avoids a paid plan; do not upgrade or add paid compute unless you choose to later. See [Supabase pricing](https://supabase.com/pricing) and [free project pausing](https://supabase.com/docs/guides/platform/free-project-pausing).

Setup steps:

1. Create a Supabase project and choose the Free plan.
2. In **Project Settings → Database**, copy the host, database name, and database user. Put them in the JDBC form `jdbc:postgresql://HOST:5432/postgres?sslmode=require`, `DATABASE_USER`, and `DATABASE_PASSWORD` in your private `.env`/host settings. If you use a pooler, use the host, port, and user format shown in Supabase’s connection panel.
3. In **Storage**, create a bucket named `roadwatch-images` and make it public; the app shows uploaded road photos on public issue pages. Do not store personal or sensitive images there.
4. In **Project Settings → API**, copy the project URL and a server-side service key into `SUPABASE_URL` and `SUPABASE_SERVICE_KEY`. The service key is private: only set it on the backend host, never in React or a committed file.
5. Keep `SUPABASE_STORAGE_BUCKET=roadwatch-images`. The app will then send image objects to Supabase and keep only their public URL in PostgreSQL. The public bucket is appropriate only for publicly viewable road photos.

If Supabase storage settings are not provided in local development, image files are written to `./uploads`, outside PostgreSQL. Production mode refuses to start with local-only image storage because free web hosts can erase local files.

### Local AI image assessment with Ollama

The app uses a locally running Ollama vision model only when `OLLAMA_URL` is set. Install Ollama, download a vision-capable model such as `llama3.2-vision`, then set `OLLAMA_URL=http://localhost:11434` and `OLLAMA_MODEL=llama3.2-vision`. If the Spring app is in Docker and Ollama runs on the host, use `http://host.docker.internal:11434`. Model downloads use local disk and compute; no paid API is called. If Ollama is unset or unavailable, an administrator sees that human review is needed. AI output is a suggestion, not a final decision.

The GitHub Actions workflow in `.github/workflows/ci.yml` builds the frontend and runs the backend unit tests on pushes and pull requests.

### Reverse geocoding and OpenStreetMap

The location button asks the browser for GPS only after the citizen clicks it. Roadwatch sends those coordinates to Nominatim to find an approximate address/area. Nominatim is a community-run service: the app limits itself to at most 1 request/second, identifies itself with `GEOCODING_USER_AGENT`, caches repeated rounded coordinates in memory, and links/credits OpenStreetMap. Supply a real contact email or site in the User-Agent and keep the service switchable. Nominatim has a strict usage policy and may change or withdraw access; for a larger deployment, run your own geocoder or choose another provider. Read the [Nominatim usage policy](https://operations.osmfoundation.org/policies/nominatim/).

## Deployment (free-tier portfolio demo)

`Dockerfile` builds the whole app as one same-origin service. `render.yaml` is a Render Blueprint starter; a hosted deployment also needs your own free Supabase project for durable PostgreSQL and image storage. Before creating anything, read the current [Render free-plan limits](https://render.com/docs/free) and [Supabase free-plan limits](https://supabase.com/pricing). Render free web services sleep after 15 idle minutes, take about a minute to wake, and lose local files on sleep/restart/redeploy; it grants 750 running instance hours per workspace each month. Its free Postgres expires after 30 days, so this setup uses Supabase for data instead. Render says excess bandwidth can be billed if a payment method is attached; without one it suspends free services when limits are reached. Do not add a payment method for this demo.

For a hosted demo, create the Supabase project and bucket first, then create a Render Blueprint from the repository. Enter the JDBC URL/user/password, admin email/password, Supabase URL, and Supabase server key as Render environment variables. Set a long unique admin password. Render’s secret fields are marked `sync: false` in `render.yaml`. Do not publish a service key. If you do not already have these accounts, create them yourself in their official dashboards; this repository contains no account credentials. Avoid clicking upgrade prompts.

This free setup is for a portfolio demonstration, not production reliability: expect cold starts, inactivity pauses, quota limits, and no automatic free database backups. Live deployment has not been performed from this workspace because it requires your accounts and private credentials.

## API outline

- `GET /api/health` — service health.
- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` — accounts/session.
- `POST /api/reports` — authenticated multipart citizen report.
- `GET /api/issues`, `GET /api/issues/{id}`, `GET /api/dashboard` — grouped issue views.
- `GET /api/my/reports` — current citizen’s reports.
- `PUT /api/admin/issues/{id}/status` — admin status update with history.
- `PUT /api/admin/reports/{id}/verification` — admin AI assessment review.
- `GET /api/locations/reverse?latitude=…&longitude=…` — address preview.

All write calls require the CSRF token managed by the React client and a signed-in session where applicable. Admin APIs additionally require the configured `ADMIN` role.

## Project structure

```text
frontend/   React, Vite, and the citizen/admin views
backend/    Spring Boot API, domain, database migrations, and storage adapters
Dockerfile  Same-origin production container build
docker-compose.yml  Local app + PostgreSQL
render.yaml Free-host deployment template (requires Supabase account settings)
```

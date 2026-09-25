# InitPrep single-host deployment

This deployment runs the existing Spring Boot services in Docker Compose on one
Linux VM. Caddy is the only public application container: it terminates HTTPS
and forwards requests to the API Gateway. PostgreSQL and all service-to-service
traffic remain on the private Compose network.

## Host requirements

- A Linux VM with a public IP, Docker Engine, and Docker Compose v2.
- A DNS name whose A/AAAA record points to that VM.
- Inbound TCP ports 80 and 443 allowed for Caddy certificate issuance and HTTPS.
- Outbound HTTPS access to OpenAI and the configured Judge0 endpoint.
- Persistent disk space and a backup plan for PostgreSQL data.

## Configure and start

1. Copy `deploy/env.production.example` to `.env` at the repository root. `.env`
   is ignored by Git. Fill in `DOMAIN`, `FRONTEND_ALLOWED_ORIGINS`,
   `POSTGRES_PASSWORD`, `JWT_SECRET`, and `OPENAI_API_KEY`. Keep the local Vite
   origins in `FRONTEND_ALLOWED_ORIGINS` and append the exact Vercel origin once
   it is known. Set a Judge0 URL and set `JUDGE0_API_KEY` only if the chosen
   Judge0 instance requires one.
2. Use a unique strong PostgreSQL password. Generate a base64 encoded random
   JWT key of at least 32 bytes and use the same `JWT_SECRET` for Auth, User,
   Interview, and Attempt services.
3. Run `docker compose -f docker-compose.production.yml up --build -d`.
4. Verify `https://<DOMAIN>/actuator/health` returns `{"status":"UP"}`, then
   verify the gateway through `/api/auth/...` and the protected question and
   attempt APIs with normal application requests.

The Compose PostgreSQL volume is named `initprep_production_postgres`; do not
remove it when updating services. The SQL bootstrap file runs only when this
volume is initialized for the first time. It creates the four service databases
and does not drop or reset existing databases. Back up this volume before host
maintenance and application upgrades.

This is a new database volume and does not migrate the current local database.
The question bank seeder is restricted to the `dev` profile, so the production
database will start without questions unless the existing data is migrated or a
separate, reviewed seed operation is run. Existing users and attempts likewise
need an intentional data migration if they must be retained.

## Service configuration

Compose supplies private DNS URLs for Gateway to Auth/User/Interview/Attempt
and Attempt to Interview/Judge/AI. The application YAML keeps localhost defaults
for local development, while deployment values come from the environment.
`JPA_DDL_AUTO` remains `update` unless explicitly overridden, preserving the
current schema behavior. SQL statement logging defaults off.

The public API base URL after DNS and HTTPS are working will be
`https://<DOMAIN>`. Use that exact URL as the frontend's `VITE_API_URL` in Vercel.

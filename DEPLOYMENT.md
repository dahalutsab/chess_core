# Deploying UChess

The web app runs on **Vercel** at `https://uchess.dahalutsab.com.np`, the API on **Render** (free plan) at
`https://api.uchess.dahalutsab.com.np`, and the database on **Neon** (free Postgres). Render's own free Postgres is
deleted after 30 days, so Neon is used instead.

```
browser ──► uchess.dahalutsab.com.np (Vercel, static Angular app)
        └─► api.uchess.dahalutsab.com.np (Render, Spring Boot in Docker) ──► Neon Postgres
```

Both sites are subdomains of `dahalutsab.com.np`, so the browser treats them as the same site: the API's login
refresh cookie (`SameSite=Lax`) reaches it from the web app.

## 1. Before anything: secrets

The repository is public, and earlier commits contain secrets that must be considered leaked:

- **Gmail app password** (formerly in `config/mail/MailConfig.java`): revoke it in the Google account
  (Security → App passwords). Removing it from the code does not remove it from git history.
- **JWT signing keys** in `src/main/resources/certs/`: these stay in the repo **for local development only**. Production
  uses new keys supplied as secret files (below), so tokens signed with the public keys are worthless there.

Generate production keys (keep them out of git):

```sh
mkdir -p ~/uchess-deploy-secrets && cd ~/uchess-deploy-secrets
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt_private_key.pem
openssl pkey -in jwt_private_key.pem -pubout -out jwt_public_key.pem
```

## 2. Database (Neon)

1. Create a project at <https://neon.tech> (region: Singapore, closest to Render's Singapore region).
2. From the connection details, note the host, database, user and password. The JDBC URL is
   `jdbc:postgresql://<host>/<database>?sslmode=require`.

Tables are created automatically on first start (`ddl-auto: update`), and the bots are inserted then.

## 3. API (Render)

1. In Render: **New → Blueprint**, connect the `chess_core` repository. Render reads `render.yaml` and creates the
   `uchess-api` web service (Docker, free plan, Singapore).
2. Fill in the secret environment variables it asks for:

   | Variable | Value |
   |---|---|
   | `DATABASE_URL` | `jdbc:postgresql://<neon host>/<db>?sslmode=require` |
   | `DATABASE_USERNAME` / `DATABASE_PASSWORD` | from Neon |
   | `ADMIN_PASSWORD` | a strong password for `admin@uchess.com` (leave empty to create no admin) |
   | `MAIL_HOST` | e.g. `smtp.gmail.com` (or your SMTP provider) |
   | `MAIL_USERNAME` / `MAIL_PASSWORD` | the mailbox and a **new** app password |
   | `MAIL_FROM` | the address emails come from, e.g. the same mailbox |

   Without working mail, people can still play as guests, but registering (which sends a verification code) fails.
3. Under the service's **Environment → Secret Files**, add two files with the contents of the keys from step 1:
   `jwt_private_key.pem` and `jwt_public_key.pem`. Render mounts them at `/etc/secrets/`, where the `prod` profile
   reads them.
4. Deploy. The health check is `GET /api/v1/bots`.
5. **Settings → Custom Domains**: `api.uchess.dahalutsab.com.np` is already listed from `render.yaml`; Render shows the
   DNS record to create (step 5) and issues the HTTPS certificate once it resolves.

Everything else (`SPRING_PROFILES_ACTIVE=prod`, the public URLs, allowed origin, engine threads) is set in
`render.yaml`. All settings are listed in `src/main/resources/application-prod.yml`.

**If you created the service with "New → Web Service" instead of a Blueprint**, `render.yaml` is not read, so add
these too under Environment:

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (the Docker image defaults to it, but set it to be explicit) |
| `PUBLIC_API_URL` | `https://api.uchess.dahalutsab.com.np` |
| `FRONTEND_URL` | `https://uchess.dahalutsab.com.np` |
| `CORS_ALLOWED_ORIGINS` | `https://uchess.dahalutsab.com.np` |
| `UCHESS_ENGINE_THREADS` | `1` |
| `UCHESS_ENGINE_HASH_MB` | `16` |

A log saying `Connection to localhost:5432 refused` means the service ran without the `prod` profile.

## 4. Web app (Vercel)

1. In Vercel: **Add New → Project**, import the `ChessBoardUI` repository. `vercel.json` sets the build
   (`npm run build`), the output folder and the rewrite that serves `index.html` for app routes.
2. The production build already points at `https://api.uchess.dahalutsab.com.np/api/v1`
   (`src/app/environment.prod.ts`).
3. **Settings → Domains**: add `uchess.dahalutsab.com.np`; Vercel shows the DNS record to create.

## 5. DNS (at the `.com.np` registrar)

| Name | Type | Value |
|---|---|---|
| `uchess` | CNAME | the target Vercel shows (typically `cname.vercel-dns.com`) |
| `api.uchess` | CNAME | the target Render shows (`uchess-api.onrender.com` or similar) |

Both services issue HTTPS certificates automatically once the records resolve.

## 6. Keeping the free API awake (optional)

Render's free plan stops the API after 15 minutes without traffic; the next visitor waits about a minute while it
starts (the web app shows "Waking up the server…"). To avoid that, a free monitor such as UptimeRobot can request
`https://api.uchess.dahalutsab.com.np/api/v1/bots` every 10 minutes. One service running all month uses about 744 of
the 750 free hours Render gives each workspace.

## Notes on the free plan

- **CPU:** the free instance has a fraction of a CPU, so bots search less in their thinking time and play weaker than
  their ratings, and engine analysis is shallower. A paid instance restores their strength.
- **Files:** there is no persistent disk; uploaded profile pictures are lost when the service restarts.
- **One instance:** the engine, live-update streams and analysis cache live in memory, so the API must run as a
  single instance (the free plan can't scale out anyway).

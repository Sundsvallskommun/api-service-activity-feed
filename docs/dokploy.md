# Deploy in Dokploy

The first version runs in [Dokploy](https://docs.dokploy.com) as a **Compose service**. `docker-compose.yml` builds
the service from source (`docker/Dockerfile`) and starts an internal MariaDB in the same stack. The database is not
published outside the stack. Only the service is reachable, through Dokploy's proxy (Traefik) and HTTPS.

```
                Dokploy (Traefik, HTTPS)
                          │
┌─────────────────────────┼──────────────────────────────┐
│ Compose stack           ▼                              │
│   db-prepare ──▶ db (MariaDB 11.4) ◀── app (:8080)     │
│      │              │                                 │
│   mariadb-backups  mariadb-data         (named volumes)│
└────────────────────────────────────────────────────────┘
```

On every deploy the services start in the order `db` → `db-prepare` (one-off job) → `app`.

## First deploy

1. **Create the service.** In the project: *Create Service* → *Compose*.
   - Provider: GitHub (or Git), the repo `Sundsvallskommun/api-service-activity-feed`, branch `main`.
   - Compose Path: `./docker-compose.yml`.
   - Compose Type: *Docker Compose*.
2. **Environment variables.** In the *Environment* tab, paste and fill in (generate the values on your own machine):

   ```bash
   # openssl rand -hex 24
   DB_PASSWORD=
   DB_ROOT_PASSWORD=
   # openssl rand -hex 32, see API keys below
   API_KEYS_ADMIN=
   API_KEYS_QUERY=
   ACTIVITY_FEED_MUNICIPALITY_ID=2281
   ACTIVITY_FEED_PRODUCER_NAME=Sundsvalls kommun
   ACTIVITY_FEED_PREFIX=SUNDSVALL
   ACTIVITY_FEED_SEED=false
   DB_BACKUP_KEEP=10
   ```

   Save. Dokploy writes the variables to a `.env` next to the compose file, and `docker-compose.yml` reads them from
   there. The deploy stops with a clear error if `DB_PASSWORD` is missing, and the service refuses to start without a
   valid API key.

3. **Domain.** In the *Domains* tab → *Add Domain*: Service Name `app`, Host e.g. `activity-feed.example.se`, Path `/`,
   Container Port `8080`, HTTPS on with Let's Encrypt. Do not publish any ports in the compose file.
   `docker-compose.local.yml` (port 8080 on the host) is only for local use and Dokploy does not use it.

4. **Deploy.** Click *Deploy* and follow the log under *Deployments*. The first build takes a few minutes while Maven
   downloads dependencies. When `app` is *healthy*:

   ```bash
   curl https://activity-feed.example.se/actuator/health
   # {"groups":["liveness","readiness"],"status":"UP"}
   curl -H "X-API-Key: <admin key>" https://activity-feed.example.se/2281/kundhandelser
   # {"antal": 0}
   ```

   In PowerShell (Windows):

   ```powershell
   Invoke-RestMethod https://activity-feed.example.se/actuator/health
   Invoke-RestMethod https://activity-feed.example.se/2281/kundhandelser -Headers @{ "X-API-Key" = "<admin key>" }
   ```

   Replace `2281` with the `ACTIVITY_FEED_MUNICIPALITY_ID` you set. The start page `/` is Swagger UI and needs no key,
   so you cannot use it to test the key.

   > **PowerShell.** There `curl` is an alias for `Invoke-WebRequest`, which does not understand curl flags like `-H`
   > and fails with *Cannot bind parameter 'Headers'*. Use `Invoke-RestMethod` as above, or type `curl.exe` to run the
   > real curl (shipped with Windows 10 and 11). With `curl.exe` the examples in this guide work unchanged, except that
   > JSON in `-d` needs different quotes. Use the PowerShell examples when a request has a body.

5. **Automatic deploy (optional).** Turn on *Autodeploy* in the *General* tab and every push to `main` deploys.
   The service has no web interface of its own besides Swagger UI on `/`.

## API keys

Every request needs the `X-API-Key` header, except `/actuator/health`, `/actuator/info` and the documentation (`/`,
`/api-docs`, `/swagger-ui/**`).

| Variable in *Environment* |                             Who uses the key                              |                                             May call                                              |
|---------------------------|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| `API_KEYS_QUERY`          | The forwarding service (Skatteverket) or whoever sends queries            | `POST /{municipalityId}/kundhandelseFragaSynkron`                                                 |
| `API_KEYS_ADMIN`          | Business system or integration platform that loads activities, operations | Everything, including `/{municipalityId}/kundhandelser/**`, `/actuator/flyway` and health details |

### Create and add keys

1. Generate one key per client on your own machine, never in a shared chat or a ticket:

   ```bash
   openssl rand -hex 32
   ```

   In PowerShell (Windows PowerShell 5.1 and PowerShell 7), with cryptographically secure randomness:

   ```powershell
   $b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); -join ($b | ForEach-Object { $_.ToString("x2") })
   ```

   A key must be at least 32 characters without spaces. `openssl rand -hex 32` gives 64 characters.

2. In Dokploy, open the *Environment* tab. Set one or more keys, comma-separated without spaces:

   ```bash
   API_KEYS_ADMIN=3f9c...e1a0
   API_KEYS_QUERY=8b27...44cd,d10e...9f3b
   ```

   `API_KEYS_ADMIN` must have at least one key. `API_KEYS_QUERY` can be empty, but give the forwarding service its own
   query key rather than an admin key. Then it cannot change the activity cache.

3. *Save* and then *Deploy*. Changed environment variables take effect on the next deploy. The data is not affected.

4. Check in the log (the *Logs* tab, service `app`) that the line
   `API key protection active with 1 admin key(s) and 2 query key(s)` is there. The keys themselves are never
   printed. Every request is logged with the key's position (`apiKeyId`, e.g. `query-2`).

5. Hand the key over to each client in a secure way. The client sends it in every request:

   ```bash
   curl https://activity-feed.example.se/2281/kundhandelseFragaSynkron \
     -H "X-API-Key: <query key>" -H 'Content-Type: application/json' \
     -H 'skv_client_correlation_id: 0002aa29-49f2-4baf-be51-c7c39c9824b4' \
     -d '{"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}]},"anvandare":"199009090000"}'
   ```

   In PowerShell:

   ```powershell
   $body = @{
     fraga     = @{ parter = @(@{ kund = @{ identifierare = "199009090000"; typ = "Personnummer" } }) }
     anvandare = "199009090000"
   } | ConvertTo-Json -Depth 10

   Invoke-RestMethod -Method Post -Uri https://activity-feed.example.se/2281/kundhandelseFragaSynkron `
     -Headers @{ "X-API-Key" = "<query key>"; "skv_client_correlation_id" = "0002aa29-49f2-4baf-be51-c7c39c9824b4" } `
     -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($body))
   ```

   `-Body` is sent as UTF-8 bytes so that å, ä and ö in titles and descriptions arrive intact, also in Windows
   PowerShell 5.1. On errors `Invoke-RestMethod` throws an exception, and the status code shows in the error message.

   Without a key or with a wrong key the response is `401`. A query key against `/kundhandelser` gives `403`.

If a client cannot send the `X-API-Key` header, `SECURITY_APIKEY_HEADERNAME` changes the name (for all keys).

Keys used in several Dokploy services can be project variables, referenced with
`API_KEYS_ADMIN=${{project.API_KEYS_ADMIN}}`.

### Rotate a key without downtime

1. Add the new key next to the old one: `API_KEYS_QUERY=<old>,<new>`. Deploy.
2. Switch the key at the client.
3. Remove the old one: `API_KEYS_QUERY=<new>`. Deploy.

If you suspect a key has leaked, remove it right away and deploy. Requests with it get `401` as soon as the new
container is up.

## Redeploy without losing data

The data lives in the named volume `mariadb-data`. *Deploy*, *Redeploy*, *Stop*/*Start*, a server restart or a new
version of the service does not remove it. This is how each deploy protects the data:

|                                                                          Protection                                                                          |               Where               |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------|
| The `mariadb-data` volume is reused. The stack publishes no database port                                                                                    | `docker-compose.yml`              |
| A database backup is taken **before** the new version starts and migrates the schema. The `DB_BACKUP_KEEP` most recent stay in the `mariadb-backups` volume  | `docker/db-prepare/db-prepare.sh` |
| `DB_PASSWORD` can change in Dokploy. The database user's password is updated on the next deploy (MariaDB otherwise only reads it when the volume is created) | `db-prepare`                      |
| Flyway only migrates forward. `clean` is disabled and migrations that already ran are validated against their checksum                                       | `application.yml`                 |
| Hibernate validates the schema but never changes it                                                                                                          | `application.yml`                 |
| Sample data loads only if the municipality has no activities, and only with `ACTIVITY_FEED_SEED=true`                                                        | `SeedService`                     |
| Graceful shutdown for the service (30 s) and the database (60 s) when containers are replaced                                                                | `docker-compose.yml`              |
| `MARIADB_AUTO_UPGRADE` upgrades the system tables when the MariaDB image gets a new version                                                                  | `docker-compose.yml`              |

After every deploy the service writes `The activity cache contains N activities for municipality 2281` to the log,
and `db-prepare` writes which backup it took. CI tests the whole flow with `docker/redeploy-test.sh`: first deploy,
loading data, a redeploy with a new build and a changed `DB_PASSWORD`, and a check of the data and the backup.

**This removes the data:**

- *Delete* of the service with *Delete volumes* checked, or `docker compose down -v` on the server.
- Changes that rename the compose project after the first deploy, e.g. the service's *App Name* or the
  *Isolated Deployment* setting. The volume name is built from the project name (`<project>_mariadb-data`), so the
  stack then gets new, empty volumes. The old ones stay on the server (`docker volume ls`) but are not used. Check the
  volume names before such a change.
- Changing `DB_ROOT_PASSWORD` after the first deploy means `db-prepare` cannot log in and the deploy stops. The data
  is still there. Set the old value back.

## Backups and restore

The backups (`activityfeed-YYYYMMDDTHHMMSSZ.sql.gz`) are in the `mariadb-backups` volume on the Dokploy server. For
copies outside the server, use Dokploy's *Volume Backups* (in the versions that have the feature) for the
`mariadb-backups` volume against an S3 destination, or copy the files with your own tools.

Run the commands below on the Dokploy server in the directory where Dokploy checked out the stack (normally
`/etc/dokploy/compose/<app-name>/code`, which holds both the compose file and `.env`). List the backups:

```bash
docker compose -p <app-name> run --rm --no-deps --entrypoint sh db-prepare -c 'ls -lh /backups'
```

Restore a backup:

1. Stop the `app` service so that nothing writes: `docker compose -p <app-name> stop app`.
2. Load the backup (replaces the database content):

   ```bash
   docker compose -p <app-name> run --rm --no-deps --entrypoint sh db-prepare -c \
     'gunzip -c /backups/activityfeed-20261001T120000Z.sql.gz | MYSQL_PWD="$DB_ROOT_PASSWORD" mariadb --host=db --user=root'
   ```
3. Start `app` again (`docker compose -p <app-name> start app`, or *Deploy* in Dokploy). If you go back to an older
   version of the service, the backup must be from before the newer version's migrations, otherwise Flyway stops the
   startup.

`<app-name>` is the service's *App Name* in Dokploy (the project name of the compose stack).

## Alternative: Application with Dokploy's database

The service can also run as an *Application* (Build Type *Dockerfile*, Docker File `docker/Dockerfile`, Build Path `/`)
against a MariaDB service created in Dokploy (*Create Service* → *Database* → *MariaDB*). Then set `DB_URL`
(`jdbc:mariadb://<the database's internal host name>:3306/<database>`), `DB_USER`, `DB_PASSWORD` and the API keys
under *Environment*, and use Dokploy's built-in database backups. `db-prepare` is not used in that setup.

# Deploy i Dokploy

Första versionen körs i [Dokploy](https://docs.dokploy.com) som en **Compose-tjänst**: `docker-compose.yml` bygger
tjänsten från källkoden (`docker/Dockerfile`) och startar en intern MariaDB i samma stack. Databasen publiceras inte
utanför stacken; bara tjänsten nås, via Dokploys proxy (Traefik) och HTTPS.

```
                Dokploy (Traefik, HTTPS)
                          │
┌─────────────────────────┼──────────────────────────────┐
│ Compose-stack           ▼                              │
│   db-prepare ──▶ db (MariaDB 11.4) ◀── app (:8080)     │
│      │              │                                 │
│   mariadb-backups  mariadb-data     (namngivna volymer)│
└────────────────────────────────────────────────────────┘
```

Vid varje deploy startar tjänsterna i ordningen `db` → `db-prepare` (engångsjobb) → `app`.

## Första deploy

1. **Skapa tjänsten.** I projektet: *Create Service* → *Compose*.
   - Provider: GitHub (eller Git), repot `Public-Service-as-a-Service/mina-arenden-ref-impl`, branch `main`.
   - Compose Path: `./docker-compose.yml`.
   - Compose Type: *Docker Compose*.
2. **Miljövariabler.** Fliken *Environment*, klistra in och fyll i (generera värden på din egen dator):

   ```bash
   # openssl rand -hex 24
   DB_PASSWORD=
   DB_ROOT_PASSWORD=
   # openssl rand -hex 32, se API-nycklar nedan
   API_KEYS_ADMIN=
   API_KEYS_FRAGA=
   MINA_ARENDEN_MUNICIPALITY_ID=2281
   MINA_ARENDEN_PRODUCENT=Sundsvalls kommun
   MINA_ARENDEN_PREFIX=SUNDSVALL
   MINA_ARENDEN_SEED=false
   DB_BACKUP_KEEP=10
   ```

   Spara. Dokploy skriver variablerna till en `.env` bredvid compose-filen, och `docker-compose.yml` hämtar dem
   därifrån. Deployen stoppas med ett tydligt fel om `DB_PASSWORD` saknas, och tjänsten vägrar starta utan en giltig
   API-nyckel.

3. **Domän.** Fliken *Domains* → *Add Domain*: Service Name `app`, Host t.ex. `mina-arenden.exempel.se`, Path `/`,
   Container Port `8080`, HTTPS på med Let's Encrypt. Publicera inga portar i compose-filen; `docker-compose.local.yml`
   (port 8080 på värden) är bara för lokal körning och används inte av Dokploy.

4. **Deploy.** Klicka *Deploy* och följ loggen under *Deployments*. Första bygget tar några minuter (Maven laddar ner
   beroenden). När `app` är *healthy*:

   ```bash
   curl https://mina-arenden.exempel.se/actuator/health
   # {"groups":["liveness","readiness"],"status":"UP"}
   curl -H "X-API-Key: <admin-nyckel>" https://mina-arenden.exempel.se/2281/kundhandelser
   # {"antal": 0}
   ```

   I PowerShell (Windows):

   ```powershell
   Invoke-RestMethod https://mina-arenden.exempel.se/actuator/health
   Invoke-RestMethod https://mina-arenden.exempel.se/2281/kundhandelser -Headers @{ "X-API-Key" = "<admin-nyckel>" }
   ```

   Byt `2281` mot det `MINA_ARENDEN_MUNICIPALITY_ID` som är satt. Startsidan `/` är Swagger UI och kräver ingen
   nyckel, så den kan inte användas för att testa nyckeln.

   > **PowerShell:** där är `curl` ett alias för `Invoke-WebRequest`, som inte förstår curl-flaggor som `-H` och ger
   > felet *Cannot bind parameter 'Headers'*. Använd `Invoke-RestMethod` som ovan, eller skriv `curl.exe` för att köra
   > riktiga curl (följer med Windows 10 och 11). Med `curl.exe` fungerar exemplen i den här guiden oförändrade, utom att
   > JSON i `-d` behöver andra citattecken; använd därför PowerShell-exemplen när en request har body.

5. **Automatisk deploy (valfritt).** Slå på *Autodeploy* i fliken *General* så deployas varje push till `main`.
   Tjänsten har inget eget webbgränssnitt utöver Swagger UI på `/`.

## API-nycklar

Alla anrop kräver headern `X-API-Key`, utom `/actuator/health`, `/actuator/info` och dokumentationen (`/`,
`/api-docs`, `/swagger-ui/**`).

| Variabel i *Environment* |                           Vem använder nyckeln                            |                                      Får anropa                                       |
|--------------------------|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `API_KEYS_FRAGA`         | Vidareförmedlingstjänsten (Skatteverket) eller den som ställer frågor     | `POST /{municipalityId}/kundhandelseFragaSynkron`                                     |
| `API_KEYS_ADMIN`         | Verksamhetssystem/integrationsplattform som läser in kundhändelser, drift | Allt, även `/{municipalityId}/kundhandelser/**`, `/actuator/flyway` och hälsodetaljer |

### Skapa och lägga in nycklar

1. Generera en nyckel per klient på din egen dator, aldrig i en delad chatt eller ett ärende:

   ```bash
   openssl rand -hex 32
   ```

   I PowerShell (Windows PowerShell 5.1 och PowerShell 7), med kryptografiskt säker slump:

   ```powershell
   $b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); -join ($b | ForEach-Object { $_.ToString("x2") })
   ```

   En nyckel ska vara minst 32 tecken utan mellanslag. `openssl rand -hex 32` ger 64 tecken.

2. I Dokploy: fliken *Environment*. Sätt en eller flera nycklar, kommaseparerade utan mellanslag:

   ```bash
   API_KEYS_ADMIN=3f9c...e1a0
   API_KEYS_FRAGA=8b27...44cd,d10e...9f3b
   ```

   `API_KEYS_ADMIN` måste ha minst en nyckel. `API_KEYS_FRAGA` kan vara tom, men ge hellre vidareförmedlingstjänsten
   en egen fråge-nyckel än en admin-nyckel: den kan då inte ändra i ärendecachen.

3. *Save* och sedan *Deploy*. Ändrade miljövariabler får effekt först vid nästa deploy. Datat påverkas inte.

4. Kontrollera i loggen (fliken *Logs*, tjänsten `app`) att raden
   `API-nyckelskydd aktivt med 1 admin-nyckel/nycklar och 2 fråge-nyckel/nycklar` finns. Nycklarna själva skrivs aldrig
   ut; varje anrop loggas med nyckelns position (`apiKeyId`, t.ex. `fraga-2`).

5. Lämna över nyckeln till respektive klient på ett säkert sätt. Klienten skickar den i varje anrop:

   ```bash
   curl https://mina-arenden.exempel.se/2281/kundhandelseFragaSynkron \
     -H "X-API-Key: <fråge-nyckel>" -H 'Content-Type: application/json' \
     -H 'skv_client_correlation_id: 0002aa29-49f2-4baf-be51-c7c39c9824b4' \
     -d '{"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}]},"anvandare":"199009090000"}'
   ```

   I PowerShell:

   ```powershell
   $body = @{
     fraga     = @{ parter = @(@{ kund = @{ identifierare = "199009090000"; typ = "Personnummer" } }) }
     anvandare = "199009090000"
   } | ConvertTo-Json -Depth 10

   Invoke-RestMethod -Method Post -Uri https://mina-arenden.exempel.se/2281/kundhandelseFragaSynkron `
     -Headers @{ "X-API-Key" = "<fråge-nyckel>"; "skv_client_correlation_id" = "0002aa29-49f2-4baf-be51-c7c39c9824b4" } `
     -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($body))
   ```

   `-Body` skickas som UTF-8-bytes så att å, ä och ö i rubriker och beskrivningar kommer fram rätt även i Windows
   PowerShell 5.1. Vid fel kastar `Invoke-RestMethod` ett undantag; statuskoden syns i felmeddelandet.

   Utan eller med fel nyckel blir svaret `401`; en fråge-nyckel mot `/kundhandelser` ger `403`.

Om en klient inte kan skicka headern `X-API-Key` kan namnet ändras med `SECURITY_APIKEY_HEADERNAME` (gäller alla
nycklar).

Nycklar som ska användas i flera Dokploy-tjänster kan läggas som projektvariabler och refereras med
`API_KEYS_ADMIN=${{project.API_KEYS_ADMIN}}`.

### Byta en nyckel utan avbrott

1. Lägg till den nya nyckeln bredvid den gamla: `API_KEYS_FRAGA=<gammal>,<ny>`. Deploy.
2. Byt nyckel hos klienten.
3. Ta bort den gamla: `API_KEYS_FRAGA=<ny>`. Deploy.

Misstänks en nyckel ha läckt: ta bort den direkt och deploya; anrop med den ger `401` så fort den nya containern är
igång.

## Omdeploy utan att data rensas

Datat ligger i den namngivna volymen `mariadb-data`. Den tas inte bort av *Deploy*, *Redeploy*, *Stop*/*Start*,
omstart av servern eller en ny version av tjänsten. Så här skyddas datat vid varje deploy:

|                                                                          Skydd                                                                           |                Var                |
|----------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------|
| Volymen `mariadb-data` återanvänds; stacken publicerar ingen databasport                                                                                 | `docker-compose.yml`              |
| Säkerhetskopia av databasen tas **innan** den nya versionen startar och migrerar schemat. De `DB_BACKUP_KEEP` senaste sparas i volymen `mariadb-backups` | `docker/db-prepare/db-prepare.sh` |
| `DB_PASSWORD` kan bytas i Dokploy: databasanvändarens lösenord uppdateras vid nästa deploy (MariaDB läser annars bara lösenordet när volymen skapas)     | `db-prepare`                      |
| Flyway migrerar bara framåt; `clean` är avstängt och redan körda migreringar valideras mot sin checksumma                                                | `application.yml`                 |
| Hibernate validerar schemat men ändrar det aldrig                                                                                                        | `application.yml`                 |
| Exempeldata läses bara in om kommunen saknar kundhändelser, och bara med `MINA_ARENDEN_SEED=true`                                                        | `SeedService`                     |
| Graceful shutdown för tjänsten (30 s) och databasen (60 s) när containrar ersätts                                                                        | `docker-compose.yml`              |
| `MARIADB_AUTO_UPGRADE` uppgraderar systemtabellerna när MariaDB-imagen får en ny version                                                                 | `docker-compose.yml`              |

Efter varje deploy skriver tjänsten `Ärendecachen innehåller N kundhändelser för kommun 2281` i loggen, och
`db-prepare` skriver vilken säkerhetskopia som togs. Hela flödet (första deploy, inläsning, omdeploy med ny build och
bytt `DB_PASSWORD`, kontroll av data och säkerhetskopia) testas i CI med `docker/redeploy-test.sh`.

**Det här tar bort datat:**

- *Delete* av tjänsten med *Delete volumes* ikryssat, eller `docker compose down -v` på servern.
- Ändringar som byter compose-projektets namn efter första deploy, t.ex. tjänstens *App Name* eller inställningen
  *Isolated Deployment*: volymnamnet bygger på projektnamnet (`<projekt>_mariadb-data`), så stacken får då nya, tomma
  volymer. De gamla finns kvar på servern (`docker volume ls`) men används inte. Kontrollera volymnamnen före en sådan
  ändring.
- Byte av `DB_ROOT_PASSWORD` efter första deploy gör att `db-prepare` inte kan logga in och deployen stoppas (datat
  finns kvar). Sätt tillbaka det gamla värdet.

## Säkerhetskopior och återställning

Säkerhetskopiorna (`minaarenden-ÅÅÅÅMMDDTHHMMSSZ.sql.gz`) ligger i volymen `mariadb-backups` på Dokploy-servern. För
kopior utanför servern: använd Dokploys *Volume Backups* (i de versioner som har funktionen) för volymen
`mariadb-backups` mot en S3-destination, eller kopiera filerna med egna verktyg.

Kommandona nedan körs på Dokploy-servern i katalogen där Dokploy har checkat ut stacken (normalt
`/etc/dokploy/compose/<app-name>/code`, där både compose-filen och `.env` finns). Lista säkerhetskopiorna:

```bash
docker compose -p <app-name> run --rm --no-deps --entrypoint sh db-prepare -c 'ls -lh /backups'
```

Återställa en säkerhetskopia:

1. Stoppa tjänsten `app` så att inga skrivningar sker: `docker compose -p <app-name> stop app`.
2. Läs in kopian (ersätter databasens innehåll):

   ```bash
   docker compose -p <app-name> run --rm --no-deps --entrypoint sh db-prepare -c \
     'gunzip -c /backups/minaarenden-20261001T120000Z.sql.gz | MYSQL_PWD="$DB_ROOT_PASSWORD" mariadb --host=db --user=root'
   ```
3. Starta `app` igen (`docker compose -p <app-name> start app`, eller *Deploy* i Dokploy). Går man tillbaka till en
   äldre version av tjänsten ska kopian vara tagen före den nyare versionens migreringar, annars stoppar Flyway starten.

`<app-name>` är tjänstens *App Name* i Dokploy (projektnamnet för compose-stacken).

## Alternativ: Application med Dokploys databas

Tjänsten kan också köras som *Application* (Build Type *Dockerfile*, Docker File `docker/Dockerfile`, Build Path `/`)
mot en MariaDB-tjänst som skapas i Dokploy (*Create Service* → *Database* → *MariaDB*). Sätt då `DB_URL`
(`jdbc:mariadb://<databasens interna värdnamn>:3306/<databas>`), `DB_USER`, `DB_PASSWORD` och API-nycklarna under
*Environment*, och använd Dokploys inbyggda databasbackuper. `db-prepare` används inte i det läget.

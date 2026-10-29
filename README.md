# Mina ärenden

_Producent enligt [Mina ärendens standard för samlad ärendeåterkoppling](https://www.digg.se/digitala-tjanster/mina-arenden---arendeaterkoppling-fran-det-offentliga),
med en ärendecache i MariaDB. Byggd som Sundsvalls kommuns api-service-tjänster (dept44, Maven, Java 25) och tänkt som
utgångspunkt för en kommun som vill lämna kundhändelser till Mina ärenden._

```
Verksamhetssystem ──POST /{municipalityId}/kundhandelser──▶ Ärendecache (MariaDB)
                                                                 │
Vidareförmedlingstjänsten (Skatteverket) ──POST /{municipalityId}/kundhandelseFragaSynkron──▶ kundhändelser
```

- **Frågegränssnitt:** `POST /{municipalityId}/kundhandelseFragaSynkron` med samma request, response och felformat
  (`{"message": "..."}`) som Skatteverkets API *Mina ärenden kundhändelser* (RAML `mina-arenden-kundhandelser` 1.0.0).
  Parter (kund, ärende eller båda), kundhändelsetyper med prefixmatchning, taggar, datumintervall, sortering och
  paginering.
- **Ärendecache:** `POST|GET|DELETE /{municipalityId}/kundhandelser` för verksamhetssystemet. Felsvar enligt RFC 9457
  (dept44 `Problem`).
- **Säkerhet:** alla anrop kräver API-nyckel i headern `X-API-Key` (se [Säkerhet](#säkerhet)).
- **Drift:** container med hälsokontroll, `docker-compose.yml` med intern MariaDB, säkerhetskopia vid varje
  omdeploy och Flyway-migreringar som aldrig tömmer databasen. Se [docs/dokploy.md](docs/dokploy.md).

## Getting Started

### Prerequisites

- **Java 25**
- **Maven 3.9.9+**
- **Docker** (Testcontainers i testerna, och för att köra med Docker Compose)
- **MariaDB** (följer med i Docker Compose)
- **Git**

### Installation

1. **Klona repot:**

```bash
git clone https://github.com/Public-Service-as-a-Service/mina-arenden-ref-impl.git
cd mina-arenden-ref-impl
```

2. **Konfigurera tjänsten.** Se [Configuration](#configuration). Utan minst en API-nyckel startar tjänsten inte.

3. **Bygg och kör.**

Med Docker Compose (bygger tjänsten och startar MariaDB):

```bash
cp .env.example .env        # sätt DB_PASSWORD, DB_ROOT_PASSWORD och API_KEYS_ADMIN (openssl rand -hex 32)
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

Med Maven mot en egen MariaDB:

```bash
export DB_URL=jdbc:mariadb://localhost:3306/minaarenden DB_USER=minaarenden DB_PASSWORD=hemligt
export API_KEYS_ADMIN=$(openssl rand -hex 32) MINA_ARENDEN_SEED=true
mvn spring-boot:run
```

Med `MINA_ARENDEN_SEED=true` läses fem exempelhändelser in vid första start
(`src/main/resources/exempel-kundhandelser.json`), så att frågor kan provas direkt.

Fler testdata, 49 kundhändelser för privatpersoner och företag i en rad olika ärendetyper, läses in via API:t med
`API_KEYS_ADMIN=<nyckel> ./tools/testdata/las-in.sh [bas-url] [kommun-id] [prefix]`. Kunderna har Skatteverkets
testpersonnummer.

### Tester

```bash
mvn verify
```

Enhetstester (`src/test`) och applikationstester (`src/integration-test`, `*IT`) körs mot MariaDB 11.4 via
Testcontainers, så Docker måste finnas. `mvn verify` kontrollerar också formateringen (`mvn dept44-formatting:apply`
rättar den) och täckningsgraden (JaCoCo, dept44-standard). `docker/redeploy-test.sh` bygger containern, gör en
omdeploy och kontrollerar att datat finns kvar; det körs i CI (`.github/workflows/docker_ci.yml`).

## Dependencies

Tjänsten har inga beroenden till andra tjänster. Den anropas av vidareförmedlingstjänsten (fråga) och av
verksamhetssystem (inläsning).

## API Documentation

- **Swagger UI:** [http://localhost:8080/](http://localhost:8080/) (även `/swagger-ui/index.html`). Knappen
  *Authorize* tar API-nyckeln.
- **OpenAPI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs). Den incheckade specifikationen
  (`src/integration-test/resources/api/openapi.yaml`) jämförs med tjänstens i `OpenApiSpecificationIT`.

## Usage

Alla sökvägar börjar med kommun-id (`2281` = Sundsvall), enligt dept44-konventionen. Kommunen måste vara konfigurerad
som producent, annars blir svaret 404. Vidareförmedlingstjänsten konfigureras med bas-URL:en
`https://<värd>/<kommun-id>`, så blir operationen `/kundhandelseFragaSynkron` som i Skatteverkets API-definition.

### Fråga om kundhändelser

```bash
curl -s http://localhost:8080/2281/kundhandelseFragaSynkron \
  -H "X-API-Key: $API_KEYS_FRAGA" \
  -H 'Content-Type: application/json' \
  -H 'skv_client_correlation_id: 0002aa29-49f2-4baf-be51-c7c39c9824b4' \
  -H 'Accept-Language: sv' \
  -d '{
    "fraga": {
      "parter": [{ "kund": { "identifierare": "199009090000", "typ": "Personnummer" } }],
      "kundhandelseTyper": ["REFKOM.BYGGLOV"],
      "startDatum": "2026-08-01",
      "slutDatum": "2026-09-30",
      "behandling": { "sortering": [{ "attribut": "TIDPUNKT", "stigande": false }], "paginering": { "offset": 0, "limit": 20 } }
    },
    "anvandare": "199009090000"
  }'
```

Svaret följer specifikationen: `kundhandelser` per part med `totaltAntalKundhandelser` och `kundhandelserForPart`,
`metadata` som ekar frågan, och `delfragor` med en rad per part (`producent`, `status`, `httpCode`). Som ensam
producent svarar tjänsten alltid `200`; vidareförmedlingstjänsten sätter `206` när någon producent fallerar.

|            Fält            |                                   Beteende                                    |
|----------------------------|-------------------------------------------------------------------------------|
| `parter[].kund` + `arende` | Båda måste stämma om båda anges                                               |
| `kundhandelseTyper`        | `REFKOM.BYGGLOV` matchar `REFKOM.BYGGLOV` och allt under (`REFKOM.BYGGLOV.*`) |
| `taggar`                   | Minst en tagg ska finnas på händelsen                                         |
| `startDatum`, `slutDatum`  | Inklusive, kl. 00:00 till 23:59:59.999 svensk tid                             |
| `behandling.sortering`     | Flera attribut i prioritetsordning; utan sortering nyast först                |
| `behandling.paginering`    | Kräver sortering; `totaltAntalKundhandelser` anger antal före paginering      |
| Fel                        | `{"message": "..."}` med 400, 401, 404, 405, 409, 415 eller 500               |

Obligatorisk header: `skv_client_correlation_id` (icke-tom sträng med skrivbara tecken, högst 200). Värdet läggs i
MDC som `skvClientCorrelationId` bredvid dept44:s `x-request-id`. Gränser per anrop (`Granser.java`): högst 100 parter,
100 kundhändelsetyper, 100 taggar och `paginering.limit` högst 1000.

### Läsa in kundhändelser i cachen

```bash
curl -s http://localhost:8080/2281/kundhandelser -H "X-API-Key: $API_KEYS_ADMIN" -H 'Content-Type: application/json' -d '[{
  "kundhandelseId": "REFKOM-BYGG-2026-00123-3",
  "part": { "kund": { "identifierare": "199009090000", "typ": "Personnummer" },
            "arende": { "identifierare": "BYGG-2026-00123", "typ": "Diarienummer" } },
  "rubrik": "Beslut om bygglov",
  "beskrivning": "Bygglov har beviljats. Beslutet finns på Mina sidor.",
  "tidpunkt": "2026-09-20T10:00:00+02:00",
  "kundhandelseTyp": "REFKOM.BYGGLOV.BESLUT",
  "producentarendetKraverKundatgard": false,
  "producentarendetKlart": true,
  "taggar": ["bygglov"],
  "referenser": { "diarienummer": "BYGG-2026-00123" }
}]'
```

Svar `201` med `{"antal": 1}`. Samma `kundhandelseId` inom kommunen ersätter befintlig post. `producent`, `sprak`
(`sv`) och `version` fylls i från konfigurationen om de utelämnas. `kundhandelseTyp` ska ha minst tre delar och börja
med kommunens prefix. Högst 1000 händelser per anrop, som sparas i en transaktion; vid 409 (samtidig inläsning av
samma nya id) kan anropet skickas om oförändrat. `tidpunkt` tas emot som RFC 3339 eller lokal tid
`ÅÅÅÅ-MM-DD HH:MM:SS` och lämnas alltid ut som RFC 3339 med svensk tidszon.

Dessutom `GET /{municipalityId}/kundhandelser/{kundhandelseId}`, `DELETE /{municipalityId}/kundhandelser/{kundhandelseId}`
och `GET /{municipalityId}/kundhandelser` (antal).

## Säkerhet

Alla anrop kräver en API-nyckel i headern `X-API-Key`, utom dokumentationen (`/`, `/api-docs`, Swagger UI) och
`/actuator/health`, `/actuator/info`.

|    Nyckel    |  Miljövariabel   |                          Får anropa                          |
|--------------|------------------|--------------------------------------------------------------|
| Fråge-nyckel | `API_KEYS_FRAGA` | `/{municipalityId}/kundhandelseFragaSynkron`                 |
| Admin-nyckel | `API_KEYS_ADMIN` | Allt: fråga, ärendecachen, `/actuator/flyway`, hälsodetaljer |

- Båda är kommaseparerade listor, så att en ny nyckel kan läggas till innan den gamla tas bort.
- Varje nyckel ska vara minst 32 tecken (`openssl rand -hex 32` ger 64). Utan någon giltig nyckel startar tjänsten
  inte; skyddet kan inte stängas av.
- Saknad eller fel nyckel ger `401`, fråge-nyckel mot ärendecachen ger `403`. Frågegränssnittet svarar i Skatteverkets
  format (`{"message":"Unauthorized"}`), övriga med RFC 9457.
- Nycklarna hålls bara som SHA-256-hashar i minnet och jämförs i konstant tid. I loggen och MDC (`apiKeyId`) syns
  bara nyckelns position, t.ex. `admin-1` eller `fraga-2`.
- Headerns namn kan ändras med `SECURITY_APIKEY_HEADERNAME`.

Hur nycklarna sätts och byts i Dokploy beskrivs i [docs/dokploy.md](docs/dokploy.md#api-nycklar).

dept44:s standardvärden loggar annars anropens innehåll (inklusive headern med API-nyckeln), exponerar alla
actuator-endpoints och visar hälsodetaljer för alla. De går före tjänstens `application.yml`, så
`SakerhetsinstallningarEnvironmentPostProcessor` lägger tjänstens värden ovanför konfigurationsfilerna: ingen
payload-loggning, bara `health`, `info` och `flyway` exponeras, och hälsodetaljer kräver admin-nyckel. Miljövariabler
går fortfarande före, t.ex. `LOGGING_LEVEL_SE_SUNDSVALL_DEPT44_PAYLOAD=TRACE` vid lokal felsökning; då loggas
headrarna, alltså API-nyckeln, men personnummer maskeras av `logbook.body-filters`. Personnummer loggas aldrig av
tjänsten själv och maskeras i `toString()` (dept44 `PiiMasker`).

## Configuration

|            Variabel             |                  Standard                   |                                Beskrivning                                |
|---------------------------------|---------------------------------------------|---------------------------------------------------------------------------|
| `DB_URL`                        | `jdbc:mariadb://localhost:3306/minaarenden` | JDBC-URL till MariaDB                                                     |
| `DB_USER`, `DB_PASSWORD`        | `minaarenden` / `minaarenden`               | Databasanvändare                                                          |
| `API_KEYS_ADMIN`                | tom                                         | Admin-nycklar, kommaseparerade. `ADMIN_API_KEY` läses om den inte är satt |
| `API_KEYS_FRAGA`                | tom                                         | Fråge-nycklar, kommaseparerade                                            |
| `MINA_ARENDEN_MUNICIPALITY_ID`  | `2281`                                      | Kommun-id för producenten                                                 |
| `MINA_ARENDEN_PRODUCENT`        | `Referenskommunen`                          | Producentens namn i svaren                                                |
| `MINA_ARENDEN_PREFIX`           | `REFKOM`                                    | Producentprefix i kundhändelsetyper (versaler A-Z)                        |
| `MINA_ARENDEN_STANDARD_VERSION` | `6.1`                                       | Standardversion som anges i `version`                                     |
| `MINA_ARENDEN_SEED`             | `false`                                     | Läs in exempelhändelser om kommunen saknar kundhändelser (endast demo)    |
| `SERVER_PORT`                   | `8080`                                      | Lyssningsport                                                             |

`MINA_ARENDEN_MUNICIPALITY_ID`, `MINA_ARENDEN_PRODUCENT` och `MINA_ARENDEN_PREFIX` fyller i den enda producenten i
`minaarenden.producenter`. Ska en instans vara producent för flera kommuner anges hela listan med indexerade
variabler, eftersom en lista från miljövariabler ersätter listan i `application.yml` i stället för att läggas till:
`MINAARENDEN_PRODUCENTER_0_MUNICIPALITYID=2281`, `MINAARENDEN_PRODUCENTER_0_NAMN=Sundsvalls kommun`,
`MINAARENDEN_PRODUCENTER_0_PREFIX=...`, `MINAARENDEN_PRODUCENTER_1_MUNICIPALITYID=2262` och så vidare.

### Database Initialization

Schemat ägs av [Flyway](https://github.com/flyway/flyway) (`src/main/resources/db/migration`) och migreras framåt vid
varje start; `clean` är avstängt och Hibernate får bara validera schemat. Ändra modellen genom att lägga till en ny
migration (och uppdatera `src/test/resources/db/schema/schema.sql`, som `SchemaVerificationTest` jämför med), aldrig
genom att ändra en befintlig: checksumman i `flyway_schema_history` stoppar annars starten.

```
kundhandelse
  id, municipality_id, kundhandelse_id (unik per kommun), producent,
  kund_identifierare, kund_typ, kund_tillagg,          -- part.kund
  arende_identifierare, arende_typ,                     -- part.arende
  rubrik, beskrivning, sprak, tidpunkt (UTC), kundhandelse_typ,
  producentarendet_kraver_kundatgard, producentarendet_klart, version,
  utokad_information (JSON-text), referenser (JSON-text), skapad, andrad
kundhandelse_tagg
  kundhandelse_ref → kundhandelse.id, tagg
```

## Deploy

Första versionen deployas till Dokploy som Compose-tjänst med intern databas. Steg för steg, API-nycklar, omdeploy,
säkerhetskopior och återställning: **[docs/dokploy.md](docs/dokploy.md)**.

## Avgränsningar

- Ingen OAuth 2. Skatteverkets vidareförmedlingstjänst autentiserar sig mot producenter enligt anslutningsvillkoren;
  API-nyckeln är ett enkelt skydd som kan kompletteras eller ersättas när det är känt.
- Auktorisation av slutanvändaren (`anvandare`) görs inte; producenten ansvarar för detta i skarp drift.
- Endast den synkrona frågan implementeras. Standardens övriga delar finns på
  [dataportal.se](https://www.dataportal.se/specifications/minaarenden/6.1).

## Källor

- [Digg: Mina ärenden](https://www.digg.se/digitala-tjanster/mina-arenden---arendeaterkoppling-fran-det-offentliga)
- [Skatteverkets utvecklarportal: API Mina ärenden kundhändelser](https://www7.skatteverket.se/portal/apier-och-oppna-data/utvecklarportalen/api/mina-arenden-kundhandelser/1.0.0/%C3%96versikt)
- [dept44](https://github.com/Sundsvallskommun/dept44)

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Code status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-activity-feed&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-activity-feed)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-activity-feed&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-activity-feed)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-activity-feed&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-activity-feed)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-activity-feed&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-activity-feed)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-activity-feed&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-activity-feed)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-activity-feed&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-activity-feed)

# Activity feed

_A producer for [Mina ärenden, the standard for consolidated case feedback](https://www.digg.se/digitala-tjanster/mina-arenden---arendeaterkoppling-fran-det-offentliga),
with an activity cache in MariaDB. Built like Sundsvalls kommun's api-service services (dept44, Maven, Java 25) and
meant as a starting point for a municipality that wants to deliver customer events (kundhändelser) to Mina ärenden._

```
Business system ──POST /{municipalityId}/kundhandelser──▶ Activity cache (MariaDB)
                                                               │
Forwarding service (Skatteverket) ──POST /{municipalityId}/kundhandelseFragaSynkron──▶ kundhändelser
```

- **Query interface.** `POST /{municipalityId}/kundhandelseFragaSynkron` uses the same request, response and error
  format (`{"message": "..."}`) as Skatteverket's API *Mina ärenden kundhändelser* (RAML `mina-arenden-kundhandelser`
  1.0.0). It supports parties (customer, errand or both), activity types with prefix matching, tags, date ranges,
  sorting and pagination.
- **Activity cache.** `POST|GET|DELETE /{municipalityId}/kundhandelser` for the business system. Errors follow
  RFC 9457 (dept44 `Problem`).

## Getting Started

### Prerequisites

- **Java 25**
- **Maven 3.9.9+**
- **Docker** (Testcontainers in the tests)
- **MariaDB**
- **Git**

### Installation

1. **Clone the repo:**

```bash
git clone https://github.com/Sundsvallskommun/api-service-activity-feed.git
cd api-service-activity-feed
```

2. **Configure the service.** See [Configuration](#configuration).

3. **Build and run** with Maven against a MariaDB. Put the local settings in
   `src/main/resources/application-default.yml`, which git ignores:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/activityfeed
    username: activityfeed
    password: secret
activity-feed:
  seed: true
```

```bash
mvn spring-boot:run
```

With `activity-feed.seed: true` the service loads five sample activities on first start
(`src/main/resources/sample-activities.json`), so you can try queries right away.

More test data, 49 activities for individuals and companies across many errand types, loads through the API with
`./tools/testdata/load.sh [base-url] [municipality-id] [prefix]`. The customers have
Skatteverket's test personal identity numbers.

### Tests

```bash
mvn verify
```

Unit tests (`src/test`) and application tests (`src/integration-test`, `*IT`) run against MariaDB 11.4 through
Testcontainers, so Docker must be available. `mvn verify` also checks the formatting (`mvn dept44-formatting:apply`
fixes it) and the coverage (JaCoCo, dept44 standard).

## Dependencies

The service calls no other services. The forwarding service (vidareförmedlingstjänsten) calls it with queries, and
business systems call it to load activities.

## API Documentation

- **Swagger UI:** [http://localhost:8080/](http://localhost:8080/) (also `/swagger-ui/index.html`).
- **OpenAPI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs). `OpenApiSpecificationIT` compares
  the service's spec with the checked-in one (`src/integration-test/resources/api/openapi.yaml`).

## Usage

Every path starts with the municipality id (`2281` = Sundsvall), following the dept44 convention. The municipality
must be configured as a producer, otherwise the response is 404. Configure the forwarding service with the base URL
`https://<host>/<municipality-id>`, so the operation becomes `/kundhandelseFragaSynkron` as in Skatteverket's API
definition.

### Query activities

```bash
curl -s http://localhost:8080/2281/kundhandelseFragaSynkron \
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

The response follows the specification. It has `kundhandelser` per party with `totaltAntalKundhandelser` and
`kundhandelserForPart`, `metadata` that echoes the query, and `delfragor` with one row per party (`producent`,
`status`, `httpCode`). As the only producer the service always answers `200`. The forwarding service sets `206` when
a producer fails.

|           Field            |                                        Behavior                                        |
|----------------------------|----------------------------------------------------------------------------------------|
| `parter[].kund` + `arende` | Both must match if both are given                                                      |
| `kundhandelseTyper`        | `REFKOM.BYGGLOV` matches `REFKOM.BYGGLOV` and everything below it (`REFKOM.BYGGLOV.*`) |
| `taggar`                   | At least one of the tags must be on the activity                                       |
| `startDatum`, `slutDatum`  | Inclusive, 00:00 to 23:59:59.999 Swedish time                                          |
| `behandling.sortering`     | Several attributes in priority order. Without sorting, newest first                    |
| `behandling.paginering`    | Requires sorting. `totaltAntalKundhandelser` gives the count before pagination         |
| Errors                     | `{"message": "..."}` with 400, 404, 405, 409, 415 or 500                               |

Required header: `skv_client_correlation_id`, a non-empty string of printable characters, at most 200. The service
puts the value in the MDC as `skvClientCorrelationId` next to dept44's `x-request-id`. Limits per request: at most
100 parties, 100 activity types, 100 tags, and `paginering.limit` at most 1000.

### Load activities into the cache

```bash
curl -s http://localhost:8080/2281/kundhandelser -H 'Content-Type: application/json' -d '[{
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

The response is `201` with `{"antal": 1}`. The same `kundhandelseId` within the municipality replaces the existing
record. The service fills in `producent`, `sprak` (`sv`) and `version` from the configuration if they are left out.
`kundhandelseTyp` must have at least three parts and start with the municipality's prefix. At most 1000 activities per
request, saved in one transaction. On 409 (a concurrent load of the same new id) the request can be sent again
unchanged. `tidpunkt` must be RFC 3339 with an offset, such as `2026-09-20T10:00:00+02:00`, and is always returned with
the Swedish offset.

There are also `GET /{municipalityId}/kundhandelser/{kundhandelseId}`,
`DELETE /{municipalityId}/kundhandelser/{kundhandelseId}` and `GET /{municipalityId}/kundhandelser` (count).

## Security

The service does no authentication of its own. dept44's `SecurityConfiguration` lets every request through, as in the
other dept44 services, so access control is up to the environment the service runs in.

Payload logging and actuator follow dept44's defaults, which `application.yml` and other properties can override.
dept44 logs request payloads with an allowlist (`logbook.body-filters.allowlist.paths`): every value in a JSON body is
masked unless its field is on the list, and the service lists none. Actuator exposes every endpoint except `heapdump`,
`threaddump` and `shutdown`, and health shows details. Since the service has no authentication, keep `/actuator` out of
reach from outside, or limit `management.endpoints.web.exposure.include`. The service itself never logs personal
identity numbers and masks them in `toString()` (dept44 `PiiMasker`).

## Configuration

|                          Property                          |      Default       |                           Description                           |
|------------------------------------------------------------|--------------------|-----------------------------------------------------------------|
| `spring.datasource.url`                                    | none               | JDBC URL to MariaDB                                             |
| `spring.datasource.username`, `spring.datasource.password` | none               | Database user                                                   |
| `activity-feed.producers[n].municipality-id`               | `2281`             | Municipality id of the producer                                 |
| `activity-feed.producers[n].name`                          | `Referenskommunen` | The producer's name in responses                                |
| `activity-feed.producers[n].prefix`                        | `REFKOM`           | Producer prefix in activity types (uppercase A-Z)               |
| `activity-feed.standard-version`                           | `6.1`              | Standard version given in `version`                             |
| `activity-feed.seed`                                       | `false`            | Load sample activities if the municipality has none (demo only) |
| `server.port`                                              | `8080`             | Listening port                                                  |

`application.yml` has a single producer, the reference municipality. For an instance that is the producer for several
municipalities, give the whole list, because a list from another properties source replaces the list in
`application.yml` instead of adding to it.

### Database Initialization

[Flyway](https://github.com/flyway/flyway) owns the schema (`src/main/resources/db/migration`) and migrates it forward
on every start. `clean` is disabled and Hibernate only validates the schema. Change the model by adding a new
migration (and update `src/test/resources/db/schema/schema.sql`, which `SchemaVerificationTest` compares against),
never by editing an existing one. Otherwise the checksum in `flyway_schema_history` stops the startup.

```
activity
  id, municipality_id, activity_id (unique per municipality), producer,
  customer_identifier, customer_type, customer_suffix,  -- part.kund
  errand_identifier, errand_type,                       -- part.arende
  title, description, language, occurred_at (UTC), activity_type,
  customer_action_required, errand_completed, version,
  extended_information (JSON text), reference_data (JSON text), created, modified
activity_tag
  activity_ref → activity.id, tag
```

## Limitations

- The service does not authenticate callers. Skatteverket's forwarding service authenticates against producers
  according to the connection terms, and that has to be handled in front of the service or added to it.
- The service does not authorize the end user (`anvandare`). The producer is responsible for that in production.
- Only the synchronous query is implemented. The other parts of the standard are on
  [dataportal.se](https://www.dataportal.se/specifications/minaarenden/6.1).

## Sources

- [Digg: Mina ärenden](https://www.digg.se/digitala-tjanster/mina-arenden---arendeaterkoppling-fran-det-offentliga)
- [Skatteverket's developer portal: API Mina ärenden kundhändelser](https://www7.skatteverket.se/portal/apier-och-oppna-data/utvecklarportalen/api/mina-arenden-kundhandelser/1.0.0/%C3%96versikt)
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

#!/bin/bash
# Verifies that a redeploy of the Compose stack does not empty the database:
#   1. first deploy (empty volume), load one activity
#   2. redeploy with a new build, new containers and a changed DB_PASSWORD (as in Dokploy)
#   3. check that the activity is still there and that db-prepare took a backup
# Used in CI (.github/workflows/docker_ci.yml) and can run locally. Requires docker compose, curl and openssl.
# The stack and its volumes are removed when the script finishes.
set -euo pipefail

cd "$(dirname "$0")/.."
export COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-activity-feed-redeploy-test}"
compose=(docker compose -f docker-compose.yml -f docker-compose.local.yml)
# Optional extra compose file, e.g. to build the app differently where the build container has no network access.
if [ -n "${COMPOSE_EXTRA_FILE:-}" ]; then
	compose+=(-f "${COMPOSE_EXTRA_FILE}")
fi

DB_PASSWORD="first-$(openssl rand -hex 12)"
DB_ROOT_PASSWORD="root-$(openssl rand -hex 12)"
API_KEYS_ADMIN="admin-$(openssl rand -hex 32)"
API_KEYS_QUERY="query-$(openssl rand -hex 32)"
ACTIVITY_FEED_SEED=false
export DB_PASSWORD DB_ROOT_PASSWORD API_KEYS_ADMIN API_KEYS_QUERY ACTIVITY_FEED_SEED

clean_up() {
	"${compose[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
}
trap clean_up EXIT
clean_up

wait_for_app() {
	for _ in $(seq 1 90); do
		if curl -fsS http://localhost:8080/actuator/health/readiness >/dev/null 2>&1; then
			return 0
		fi
		sleep 2
	done
	"${compose[@]}" logs app db-prepare | tail -100
	echo "The service never became ready" >&2
	return 1
}

echo "== First deploy"
"${compose[@]}" up -d --build
wait_for_app

curl -fsS -X POST http://localhost:8080/2281/kundhandelser \
	-H "X-API-Key: ${API_KEYS_ADMIN}" -H 'Content-Type: application/json' \
	-d '[{"kundhandelseId":"REFKOM-REDEPLOY-1","part":{"arende":{"identifierare":"R-1","typ":"Diarienummer"}},
	      "rubrik":"Ska överleva omdeploy","beskrivning":"b","tidpunkt":"2026-09-01T10:00:00+02:00",
	      "kundhandelseTyp":"REFKOM.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]'
echo

echo "== Redeploy with a new build and a changed DB_PASSWORD"
DB_PASSWORD="second-$(openssl rand -hex 12)"
export DB_PASSWORD
"${compose[@]}" up -d --build --force-recreate
wait_for_app

title=$(curl -fsS http://localhost:8080/2281/kundhandelser/REFKOM-REDEPLOY-1 -H "X-API-Key: ${API_KEYS_ADMIN}" | grep -o '"rubrik" *: *"[^"]*"')
echo "After redeploy: ${title}"
[[ "${title}" == *"Ska överleva omdeploy"* ]] || {
	echo "The activity is missing after redeploy" >&2
	exit 1
}

# The query interface with a query key, and 401 without a key.
curl -fsS -o /dev/null http://localhost:8080/2281/kundhandelseFragaSynkron -H "X-API-Key: ${API_KEYS_QUERY}" \
	-H 'Content-Type: application/json' -H 'skv_client_correlation_id: redeploy-test' \
	-d '{"fraga":{"parter":[{"arende":{"identifierare":"R-1","typ":"Diarienummer"}}]},"anvandare":"199009090000"}'
status=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/2281/kundhandelser)
[ "${status}" = "401" ] || {
	echo "Expected 401 without an API key, got ${status}" >&2
	exit 1
}

backups=$("${compose[@]}" run --rm --no-deps --entrypoint sh db-prepare -c 'ls -1 /backups/*.sql.gz 2>/dev/null | wc -l')
echo "Backups: ${backups}"
[ "${backups}" -ge 1 ] || {
	echo "No backup was taken on redeploy" >&2
	exit 1
}

echo "OK: the data is still there after redeploy"

#!/bin/bash
# Läser in testdatat i kundhandelser.json i ärendecachen via POST /{municipalityId}/kundhandelser.
#
#   API_KEYS_ADMIN=<admin-nyckel> ./tools/testdata/las-in.sh [bas-url] [kommun-id] [prefix]
#
# Standard är http://localhost:8080, kommun 2281 och prefixet REFKOM. Filen är skriven med REFKOM, som byts mot
# kommunens prefix i kundhandelseId och kundhandelseTyp. Samma kundhandelseId ersätter befintlig post, så skriptet kan
# köras flera gånger.
#
# Kunderna har Skatteverkets testpersonnummer. Organisationsnumren har medvetet fel kontrollsiffra, så att de inte kan
# tillhöra något riktigt företag.
set -euo pipefail

cd "$(dirname "$0")"
bas_url="${1:-http://localhost:8080}"
kommun="${2:-2281}"
prefix="${3:-REFKOM}"
: "${API_KEYS_ADMIN:?Sätt API_KEYS_ADMIN till en admin-nyckel}"

# API_KEYS_ADMIN kan vara en kommaseparerad lista; första nyckeln används.
sed -e "s/\"REFKOM\([.-]\)/\"${prefix}\1/g" kundhandelser.json |
	curl -fsS -X POST "${bas_url}/${kommun}/kundhandelser" \
		-H "X-API-Key: ${API_KEYS_ADMIN%%,*}" -H 'Content-Type: application/json' --data-binary @-
echo

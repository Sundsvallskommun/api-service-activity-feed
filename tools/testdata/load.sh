#!/bin/bash
# Loads the test data in activities.json into the activity cache through POST /{municipalityId}/kundhandelser.
#
#   API_KEYS_ADMIN=<admin key> ./tools/testdata/load.sh [base-url] [municipality-id] [prefix]
#
# Defaults are http://localhost:8080, municipality 2281 and the prefix REFKOM. The file uses REFKOM, which is replaced
# by the municipality's prefix in kundhandelseId and kundhandelseTyp. The same kundhandelseId replaces the existing
# record, so the script can run several times.
#
# The customers have Skatteverket's test personal identity numbers. The organisation numbers have a deliberately wrong
# check digit, so they cannot belong to a real company.
set -euo pipefail

cd "$(dirname "$0")"
base_url="${1:-http://localhost:8080}"
municipality="${2:-2281}"
prefix="${3:-REFKOM}"
: "${API_KEYS_ADMIN:?Set API_KEYS_ADMIN to an admin key}"

# API_KEYS_ADMIN may be a comma-separated list; the first key is used.
sed -e "s/\"REFKOM\([.-]\)/\"${prefix}\1/g" activities.json |
	curl -fsS -X POST "${base_url}/${municipality}/kundhandelser" \
		-H "X-API-Key: ${API_KEYS_ADMIN%%,*}" -H 'Content-Type: application/json' --data-binary @-
echo

#!/bin/bash
# Runs as a one-off service (db-prepare) on every deploy, after the database is up and before the application starts.
#
# 1. Backs up the database to the mariadb-backups volume (if it contains tables), so that a redeploy with a new
#    version, including new Flyway migrations, can always be rolled back.
# 2. Removes old backups so that at most DB_BACKUP_KEEP remain.
# 3. Sets the password of the application's database user to DB_PASSWORD. The MariaDB image only reads
#    MARIADB_PASSWORD when the volume is initialised the first time. Without this step, a changed DB_PASSWORD in
#    Dokploy would lock the application out after the next deploy.
#
# This script never changes the data in the database.
set -euo pipefail

: "${DB_HOST:=db}"
: "${DB_NAME:=activityfeed}"
: "${DB_USER:=activityfeed}"
: "${DB_BACKUP_DIR:=/backups}"
: "${DB_BACKUP_KEEP:=10}"
: "${DB_PASSWORD:?DB_PASSWORD must be set}"
: "${DB_ROOT_PASSWORD:?DB_ROOT_PASSWORD must be set}"

export MYSQL_PWD="${DB_ROOT_PASSWORD}"
client=(mariadb --host="${DB_HOST}" --user=root --batch --skip-column-names)

for attempt in $(seq 1 30); do
	if "${client[@]}" -e "SELECT 1" >/dev/null 2>&1; then
		break
	fi
	if [ "${attempt}" -eq 30 ]; then
		echo "db-prepare: cannot connect to ${DB_HOST} as root. Has DB_ROOT_PASSWORD changed after the first deploy?" >&2
		exit 1
	fi
	sleep 2
done

table_count=$("${client[@]}" -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DB_NAME}'")
if [ "${table_count}" -gt 0 ]; then
	mkdir -p "${DB_BACKUP_DIR}"
	file="${DB_BACKUP_DIR}/${DB_NAME}-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"
	mariadb-dump --host="${DB_HOST}" --user=root --single-transaction --routines --triggers --databases "${DB_NAME}" | gzip > "${file}.tmp"
	mv "${file}.tmp" "${file}"
	echo "db-prepare: backup saved: ${file} ($(du -h "${file}" | cut -f1))"
	# Keep the DB_BACKUP_KEEP most recent backups.
	ls -1t "${DB_BACKUP_DIR}/${DB_NAME}-"*.sql.gz 2>/dev/null | tail -n +"$((DB_BACKUP_KEEP + 1))" | xargs -r rm -f --
else
	echo "db-prepare: database ${DB_NAME} is empty (first deploy), no backup needed"
fi

password=${DB_PASSWORD//\\/\\\\}
password=${password//\'/\\\'}
"${client[@]}" -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${password}';
ALTER USER '${DB_USER}'@'%' IDENTIFIED BY '${password}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'%';"
echo "db-prepare: database user ${DB_USER} is updated"

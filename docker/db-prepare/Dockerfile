# One-off job that runs on every (re)deploy before the application starts. It backs up the database and makes sure the
# application's database user has the password from DB_PASSWORD. See docker/db-prepare/db-prepare.sh.
FROM mariadb:11.4
COPY db-prepare.sh /usr/local/bin/db-prepare.sh
RUN chmod 0755 /usr/local/bin/db-prepare.sh
ENTRYPOINT ["/usr/local/bin/db-prepare.sh"]

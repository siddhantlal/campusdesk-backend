#!/bin/sh
set -eu

# Render supplies a postgresql:// URI, while JDBC requires jdbc:postgresql://.
# User/password are injected separately; stripping them avoids putting secrets in the JDBC URL.
if [ -z "${DB_URL:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
    case "$DATABASE_URL" in
        postgresql://*|postgres://*) ;;
        *) echo 'DATABASE_URL must be a PostgreSQL URL' >&2; exit 1 ;;
    esac
    connection="${DATABASE_URL#*://}"
    host_and_database="${connection##*@}"
    case "$host_and_database" in
        *\?*) export DB_URL="jdbc:postgresql://${host_and_database}&sslmode=require" ;;
        *) export DB_URL="jdbc:postgresql://${host_and_database}?sslmode=require" ;;
    esac
fi

exec java -jar /app/app.jar

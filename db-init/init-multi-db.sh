#!/bin/bash
# Creates multiple logical databases on first container init.
# POSTGRES_DB / POSTGRES_USER create only one database by default,
# so this script creates every DB listed in POSTGRES_MULTIPLE_DATABASES.
set -e

if [ -z "$POSTGRES_MULTIPLE_DATABASES" ]; then
  echo "POSTGRES_MULTIPLE_DATABASES not set — skipping extra database creation."
  exit 0
fi

# The maintenance DB always exists (defaults to $POSTGRES_USER).
MAINT_DB="${POSTGRES_DB:-$POSTGRES_USER}"

for db in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
  echo "Creating database '$db'..."
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$MAINT_DB" \
    -c "SELECT 1 FROM pg_database WHERE datname = '$db'" \
    | grep -q 1 || psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" \
    --dbname "$MAINT_DB" -c "CREATE DATABASE \"$db\";"
done

echo "Multiple databases created: $POSTGRES_MULTIPLE_DATABASES"

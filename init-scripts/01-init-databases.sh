#!/bin/sh
set -e

[ -z "$POSTGRES_MULTIPLE_DATABASES" ] && exit 0

for db in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
    echo "Creating database '$db'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -c "CREATE DATABASE $db;"
done
echo "Databases created."

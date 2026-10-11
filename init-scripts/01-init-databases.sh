#!/bin/sh
set -e

create_user_and_database() {
    local database=$1
    echo "  Creating database '$database' using user '$REAL_USER'"

    psql -v ON_ERROR_STOP=1 --username "$REAL_USER" --dbname "postgres" <<-EOSQL
        CREATE DATABASE $database;
EOSQL
}

if [ -f "/run/secrets/postgres_user" ]; then
    REAL_USER=$(cat /run/secrets/postgres_user)
else
    echo "Error: Secret /run/secrets/postgres_user not found!"
    exit 1
fi

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "Multiple databases creation requested: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
        create_user_and_database "$db"
    done
    echo "Multiple databases created successfully!"
fi

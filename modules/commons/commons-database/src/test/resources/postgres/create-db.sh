#!/usr/bin/env bash

set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER test WITH PASSWORD 'changeMe';
    CREATE DATABASE online_retailer;
    GRANT ALL PRIVILEGES ON DATABASE online_retailer TO test;
    \connect online_retailer "$POSTGRES_USER"
    GRANT USAGE, CREATE ON SCHEMA public TO test;
EOSQL
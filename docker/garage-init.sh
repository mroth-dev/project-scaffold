#!/usr/bin/env bash
# Bootstraps the local Garage container: assigns the single-node layout,
# then creates the bucket and a fixed access/secret key pair so local dev
# credentials never change between runs. Safe to re-run - every step
# tolerates "already exists"/"already assigned" errors.
#
# Run this once after `docker-compose up -d` (or after wiping the
# garage_meta/garage_data volumes). The garage image itself has no shell,
# which is why this can't be a docker-compose service - it drives the
# already-running container via `docker exec` from the host instead.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
set -a
source .env
set +a

CONTAINER=ecommerce-garage

echo "Waiting for $CONTAINER to be healthy..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null)" = "healthy" ]; do
    sleep 1
done

NODE_ID=$(docker exec "$CONTAINER" /garage status | awk '/NO ROLE ASSIGNED/ {print $1; exit}')
if [ -n "$NODE_ID" ]; then
    echo "Assigning single-node layout to $NODE_ID..."
    docker exec "$CONTAINER" /garage layout assign -z dc1 -c 1G "$NODE_ID"
    docker exec "$CONTAINER" /garage layout apply --version 1
else
    echo "Layout already assigned, skipping."
fi

echo "Creating bucket ${GARAGE_BUCKET}..."
docker exec "$CONTAINER" /garage bucket create "$GARAGE_BUCKET" || true

echo "Importing access key ${GARAGE_ACCESS_KEY}..."
docker exec "$CONTAINER" /garage key import "$GARAGE_ACCESS_KEY" "$GARAGE_SECRET_KEY" -n app-key --yes || true

echo "Granting read/write access..."
docker exec "$CONTAINER" /garage bucket allow --read --write "$GARAGE_BUCKET" --key "$GARAGE_ACCESS_KEY"

echo "Done."

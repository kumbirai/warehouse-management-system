#!/bin/bash

set -e

echo "Stopping development services..."

cd infrastructure/docker
docker-compose -f docker-compose.dev.yml down

echo "Services stopped."


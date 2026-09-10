#!/bin/bash
set -e
export DEBIAN_FRONTEND=noninteractive

echo "[setup.sh] Starting Linuxxx database package installation..."

apt-get update
apt-get install -y --no-install-recommends ca-certificates curl gnupg

# Add official MongoDB 7.0 ARM64 repository for Ubuntu 24.04 (noble)
mkdir -p /usr/share/keyrings
curl -fsSL https://www.mongodb.org/static/pgp/server-7.0.asc | \
   gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor --yes || true

echo "deb [ signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu noble/mongodb-org/7.0 multiverse" \
   | tee /etc/apt/sources.list.d/mongodb-org-7.0.list

apt-get update

# Install MariaDB 11.x, Redis 7.x, and MongoDB 7.x simultaneously
if ! apt-get install -y --no-install-recommends mariadb-server redis-server mongodb-org; then
    echo "[setup.sh] MongoDB 7.0 install failed, attempting fallback to mongodb package from universe..."
    apt-get install -y --no-install-recommends mariadb-server redis-server mongodb || apt-get install -y --no-install-recommends mariadb-server redis-server
fi

# Ensure all runtime and data directories exist
mkdir -p /root/workspace
mkdir -p /var/lib/mysql /var/lib/redis /var/lib/mongodb
mkdir -p /var/run/mysqld /var/run/redis /var/run/mongodb
mkdir -p /var/log/mysql /var/log/redis /var/log/mongodb

echo "[setup.sh] Setup completed successfully!"

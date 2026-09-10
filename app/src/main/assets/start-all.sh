#!/bin/bash
set -e
trap 'kill 0' EXIT INT TERM

echo "[start-all.sh] Initializing runtime directories..."
mkdir -p /var/run/mysqld /var/run/redis /var/run/mongodb
mkdir -p /var/lib/mysql /var/lib/redis /var/lib/mongodb
mkdir -p /var/log/mysql /var/log/redis /var/log/mongodb
chown -R root:root /var/lib/mysql /var/lib/redis /var/lib/mongodb 2>/dev/null || true

# Initialize MariaDB data directory if first run
if [ ! -d /var/lib/mysql/mysql ]; then
    echo "[start-all.sh] Bootstrapping MariaDB system tables..."
    mariadb-install-db --user=root --datadir=/var/lib/mysql 2>/dev/null || mysql_install_db --user=root --datadir=/var/lib/mysql 2>/dev/null || true
fi

# Start MariaDB 11.x
echo "[start-all.sh] Starting MariaDB on port 3306..."
mysqld_safe --datadir=/var/lib/mysql \
  --socket=/var/run/mysqld/mysqld.sock \
  --port=3306 --bind-address=127.0.0.1 &

# Start Redis 7.x
echo "[start-all.sh] Starting Redis on port 6379..."
redis-server /etc/redis/redis.conf &

# Start MongoDB 7.x (or universe fallback)
echo "[start-all.sh] Starting MongoDB on port 27017..."
if command -v mongod >/dev/null 2>&1; then
    mongod --config /etc/mongod.conf &
elif command -v mongodb >/dev/null 2>&1; then
    mongodb --config /etc/mongod.conf &
else
    echo "[start-all.sh] Warning: mongod binary not found in PATH. Run setup.sh to install."
fi

echo "[start-all.sh] All daemons launched! Waiting for background processes..."
wait

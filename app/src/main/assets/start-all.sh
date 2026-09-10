#!/bin/bash
set -e
trap 'kill 0' EXIT INT TERM

echo "[start-all.sh] Initializing runtime directories..."
mkdir -p /var/run/mysqld /var/run/redis /var/run/mongodb
mkdir -p /var/lib/mysql /var/lib/redis /var/lib/mongodb
mkdir -p /var/log/mysql /var/log/redis /var/log/mongodb
chown -R root:root /var/lib/mysql /var/lib/redis /var/lib/mongodb 2>/dev/null || true

# Initialize MariaDB data directory if first run
FIRST_RUN_MARIADB=0
if [ ! -d /var/lib/mysql/mysql ]; then
    FIRST_RUN_MARIADB=1
    echo "[start-all.sh] Bootstrapping MariaDB system tables..."
    mariadb-install-db --user=root --datadir=/var/lib/mysql 2>/dev/null || mysql_install_db --user=root --datadir=/var/lib/mysql 2>/dev/null || true
fi

# Start MariaDB 11.x
echo "[start-all.sh] Starting MariaDB on port 3306..."
mysqld_safe --datadir=/var/lib/mysql \
  --socket=/var/run/mysqld/mysqld.sock \
  --port=3306 --bind-address=127.0.0.1 &
MARIADB_PID=$!

# Secure root user password on first run or when MARIADB_ROOT_PASSWORD is provided
(
  if [ -n "$MARIADB_ROOT_PASSWORD" ]; then
    echo "[start-all.sh] Waiting for MariaDB socket to secure root user..."
    for i in $(seq 1 30); do
      if [ -S /var/run/mysqld/mysqld.sock ] || mysqladmin ping --socket=/var/run/mysqld/mysqld.sock --silent 2>/dev/null; then
        echo "[start-all.sh] MariaDB socket is ready. Securing root account..."
        # Set root password for 127.0.0.1 and localhost
        mariadb -u root --socket=/var/run/mysqld/mysqld.sock -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$MARIADB_ROOT_PASSWORD'; ALTER USER 'root'@'127.0.0.1' IDENTIFIED BY '$MARIADB_ROOT_PASSWORD'; FLUSH PRIVILEGES;" 2>/dev/null || \
        mysql -u root --socket=/var/run/mysqld/mysqld.sock -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$MARIADB_ROOT_PASSWORD'; ALTER USER 'root'@'127.0.0.1' IDENTIFIED BY '$MARIADB_ROOT_PASSWORD'; FLUSH PRIVILEGES;" 2>/dev/null || true
        echo "[start-all.sh] MariaDB root user secured."
        break
      fi
      sleep 1
    done
  fi
) &

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

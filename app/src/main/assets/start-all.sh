#!/bin/bash
set -e
trap 'kill 0' EXIT INT TERM

mkdir -p /var/run/mysqld /var/run/redis /var/run/mongodb
chown -R root:root /var/lib/mysql /var/lib/redis /var/lib/mongodb 2>/dev/null || true

# Initialize MariaDB if first run
if [ ! -d /var/lib/mysql/mysql ]; then
    mariadb-install-db --user=root --datadir=/var/lib/mysql
fi

# Initialize Redis dir
mkdir -p /var/lib/redis

# Initialize MongoDB dir
mkdir -p /var/lib/mongodb

# Start MariaDB
mysqld_safe --datadir=/var/lib/mysql \
  --socket=/var/run/mysqld/mysqld.sock \
  --port=3306 --bind-address=127.0.0.1 &

# Start Redis
redis-server /etc/redis/redis.conf &

# Start MongoDB
mongod --config /etc/mongod.conf &

wait

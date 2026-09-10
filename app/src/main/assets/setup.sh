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
    echo "[setup.sh] MongoDB 7.0 install failed (likely pre-ARMv8.2-A device without LSE). Attempting fallback to MongoDB 4.4 ARM64 repository..."
    rm -f /etc/apt/sources.list.d/mongodb-org-7.0.list
    
    # Download and install libssl1.1 directly from official Ubuntu ports without repository mixing
    echo "[setup.sh] Downloading and installing libssl1.1 directly..."
    curl -fsSL -O http://ports.ubuntu.com/pool/main/o/openssl/libssl1.1_1.1.1f-1ubuntu2_arm64.deb
    dpkg -i libssl1.1_1.1.1f-1ubuntu2_arm64.deb || apt-get install -fy
    rm -f libssl1.1_1.1.1f-1ubuntu2_arm64.deb
    
    # Download 4.4 key
    curl -fsSL https://www.mongodb.org/static/pgp/server-4.4.asc | \
       gpg -o /usr/share/keyrings/mongodb-server-4.4.gpg --dearmor --yes || true
       
    echo "deb [ signed-by=/usr/share/keyrings/mongodb-server-4.4.gpg ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/4.4 multiverse" \
       | tee /etc/apt/sources.list.d/mongodb-org-4.4.list
       
    apt-get update || true
    apt-get install -y --no-install-recommends mariadb-server redis-server mongodb-org || \
       apt-get install -y --no-install-recommends mariadb-server redis-server
fi

# Ensure all runtime and data directories exist
mkdir -p /root/workspace
mkdir -p /var/lib/mysql /var/lib/redis /var/lib/mongodb
mkdir -p /var/run/mysqld /var/run/redis /var/run/mongodb
mkdir -p /var/log/mysql /var/log/redis /var/log/mongodb

echo "[setup.sh] Setup completed successfully!"

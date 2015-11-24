#!/bin/bash
# Starts up MariaDB within the container.

# Stop on error
set -e

DATA_DIR=/data

if [[ -e /firstrun ]]; then
  source /scripts/first_run.sh
else
  source /scripts/normal_run.sh
fi

wait_for_mysql_and_run_post_start_action() {
  # Wait for mysql to finish starting up first.
  while [[ ! -e /run/mysqld/mysqld.sock ]] ; do
      inotifywait -q -e create /run/mysqld/ >> /dev/null
  done

  post_start_action
}

pre_start_action

wait_for_mysql_and_run_post_start_action &

# Start MariaDB
echo "Starting MariaDB..."
exec /usr/bin/mysqld_safe

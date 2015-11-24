#MariaDB (https://mariadb.org/)

FROM phusion/baseimage:0.9.10
MAINTAINER me <me>

# Ensure UTF-8
RUN locale-gen en_US.UTF-8

# Disable SSH (Not using it at the moment).
RUN rm -rf /etc/service/sshd /etc/my_init.d/00_regen_ssh_host_keys.sh

# Install MariaDB from repository.
RUN echo "deb http://ftp.osuosl.org/pub/mariadb/repo/10.1/ubuntu trusty main" > /etc/apt/sources.list.d/mariadb.list && \
    apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --force-yes mariadb-server mariadb-server-10.1

# Install other tools.
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y pwgen inotify-tools

# Clean up APT when done.
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Configure the database to use our data dir.
RUN sed -i -e 's/^datadir\s*=.*/datadir = \/data/' /etc/mysql/my.cnf

# Configure MariaDB to listen on any address.
RUN sed -i -e 's/^bind-address/#bind-address/' /etc/mysql/my.cnf

# Change the innodb-buffer-pool-size to 128M (default is 256M).
# This should make it friendlier to run on low memory servers.
RUN sed -i -e 's/^innodb_buffer_pool_size\s*=.*/innodb_buffer_pool_size = 128M/' /etc/mysql/my.cnf

EXPOSE 3306
ADD scripts /scripts
RUN chmod +x /scripts/start.sh
RUN touch /firstrun

# Expose our data, log, and configuration directories.
VOLUME ["/data", "/var/log/mysql", "/etc/mysql"]

# Use baseimage-docker's init system.
CMD ["/sbin/my_init", "--", "/scripts/start.sh"]
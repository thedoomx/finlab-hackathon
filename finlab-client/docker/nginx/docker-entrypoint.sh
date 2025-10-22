#!/usr/bin/env sh
set -eu

if [ "${DOCKER:-false}" = false ]; then
    cp /etc/nginx/conf.d/default.conf.template /etc/nginx/conf.d/default.conf
else
    cp /etc/nginx/conf.d/default.conf.docker.template /etc/nginx/conf.d/default.conf
fi

exec "$@"

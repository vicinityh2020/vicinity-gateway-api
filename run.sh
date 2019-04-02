#!/bin/sh

# BUILD GATEWAY
docker kill gtw-api >/dev/null 2>&1
docker rm gtw-api >/dev/null 2>&1
docker rmi gtw-api >/dev/null 2>&1
docker build -f Dockerfile.gateway -t gtw-api .

# RUN GATEWAY
docker run -d -p 8181:8181 -v ~/docker_data/logs/gateway:/gateway/log -it --rm --name gtw-api gtw-api:latest

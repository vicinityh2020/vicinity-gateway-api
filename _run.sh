#!/bin/sh

# BUILDS GATEWAY FROM DEVELOP BRANCH - USE FOR TESTING BEFORE PUSHING TO DEVELOPMENT
# RUNNING WITH DOCKER

MY_PATH=$(pwd)

# BUILD GATEWAY
docker kill vcnt-gateway >/dev/null 2>&1
docker rm vcnt-gateway >/dev/null 2>&1
docker rmi vcnt-gateway >/dev/null 2>&1
docker build --build-arg MY_ENV=develop --build-arg UID=1001 --build-arg GID=1001 -t vcnt-gateway .

# CREATE FOLDERS
mkdir -p ${MY_PATH}/log

# RUN GATEWAY
docker run -d -p 8181:8181 -v ${MY_PATH}/log:/gateway/log -it --rm --name vcnt-gateway vcnt-gateway:latest

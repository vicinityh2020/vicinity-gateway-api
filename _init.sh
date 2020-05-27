#!/bin/sh

# RUN GATEWAY USING YOUR LOCAL JAVA ENV

MY_PATH=$(pwd)

# Create directories
mkdir -p ${MY_PATH}/data
mkdir -p ${MY_PATH}/log

# Start locally
java -jar ${MY_PATH}/target/ogwapi-jar-with-dependencies.jar
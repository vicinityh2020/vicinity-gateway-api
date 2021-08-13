#!/bin/sh

#!/bin/bash
USAGE="$(basename "$0") [-h -e]
Gateway initialization script.
Run the gateway using one of the following options:
-- jar: Run using java environment installed in your computer
-- test: Run test suite
-- docker local: Build the local docker image of your source code
-- OPTIONS:
./_run.sh -e jar --> jar mode
./_run.sh -e test --> run tests
./_run.sh -e local --> docker local mode
./_run.sh -e build --> build jar files
Where:
  Flags:
      -h  Shows help
      -e  environment"

# Consts & Variables

MY_PATH=$(pwd)
MY_ENV="NONE"

# Get arguments

while getopts 'hd:e:' OPTION; do
case "$OPTION" in
    h)
    echo "$USAGE"
    exit 0
    ;;
    e)
    MY_ENV="$OPTARG"
    ;;
esac
done

# Create directories

mkdir -p ${MY_PATH}/data
mkdir -p ${MY_PATH}/log

if [ ${MY_ENV} == "jar" ]; then

    # Start locally
    echo "For security reasons do not run this process as root user in production"
    read -t 5 -p "..."
    java -jar ${MY_PATH}/target/ogwapi-jar-with-dependencies.jar

elif [ ${MY_ENV} == "local" ]; then

    read -p "Do you want to rebuild the image? " -n 1 -r
    echo    # (optional) move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        # Clean docker old builds and build locally
        docker kill vcnt-gateway >/dev/null 2>&1
        docker rm vcnt-gateway >/dev/null 2>&1
        docker rmi vcnt-gateway >/dev/null 2>&1
        docker build --build-arg MY_ENV=develop --build-arg UID=1001 --build-arg GID=1001 -t vcnt-gateway .
    fi
    docker run -d -p 8181:8181 -v ${MY_PATH}/log:/gateway/log -it --rm --name vcnt-gateway vcnt-gateway:latest

elif [ ${MY_ENV} == "test" ]; then

    docker run -v $(pwd)/config:/libs -p 1080:1080  mockserver/mockserver -serverPort 1080
    mvn test

elif [ ${MY_ENV} == "test" ]; then

    echo "Remember that tests use VICINITY base URI: /commserver/"
    docker run -v $(pwd)/config:/libs -p 1080:1080  mockserver/mockserver -serverPort 1080
    mvn clean package

else 
    echo "Please choose some environment"
fi




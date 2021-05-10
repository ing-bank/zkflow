#!/bin/bash

# this forces the bash script to stop processing as soon as it hits an error
set -e

####### PARAMS - START ########

DOCKER_TAG=${1}

if [ "$1" == "" ]; then
    DOCKER_TAG="latest"
fi

####### PARAMS - END ########

# get version number from the version.txt file
#   this is SNAPSHOT by default - or it will have been overwritten by the build process
#     with the actual version for that build
IVNO_VERSION=$(cat version.txt) # e.g. master+35 or 1.2.45+53

docker build --build-arg IVNO_VERSION="${IVNO_VERSION}" -f Dockerfile -t ivnocorda:${DOCKER_TAG} .

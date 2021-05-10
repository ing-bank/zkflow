#!/bin/bash

# this forces the bash script to stop processing as soon as it hits an error
set -e

####### PARAMS - START ########

DOCKER_TAG=${1}

if [ "$1" == "" ]; then
    DOCKER_TAG="latest"
fi

####### PARAMS - END ########

docker build -f Dockerfile_cordabase -t ivnocordabase:${DOCKER_TAG} .

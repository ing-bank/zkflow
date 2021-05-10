#!/usr/bin/env bash

# this forces the bash script to stop processing as soon as it hits an error
set -e

# build (no tests)
./gradlew clean build -x test

# get rid of what is in maven local
./clean-maven-local.sh

# publish to maven local
./gradlew publishToMavenLocal

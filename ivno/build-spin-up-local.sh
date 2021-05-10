#!/usr/bin/env bash

# this forces the bash script to stop processing as soon as it hits an error
set -e

./build-deploy-local.sh

./spin-up-deploy-nodes.sh

#!/usr/bin/env bash
cd $(git rev-parse --show-toplevel)/pepper

docker run --rm -v /"$(pwd)":/opt/pequin/pepper -it mvdbos/corda-zk-notary bash
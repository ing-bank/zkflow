#!/usr/bin/env bash

cd $(git rev-parse --show-toplevel)

test_name=$1

# if test_name is empty, set it to 'com.ing.zknotary.flows.SimpleZKNotaryFlowTest'
if [[ -z "$test_name" ]]
then
      test_name="com.ing.zknotary.notary.transactions.ComplexZKProofTest"
fi

# Mounting pepper dir is necessary for our gadget to be able to find the .arith files.
docker run \
    --rm \
    -v "$(pwd)":/src  \
    -v "$(pwd)/pepper":/opt/pequin/pepper  \
    -v ~/.gradle/caches:/root/.gradle/caches \
    -v ~/.m2/repository:/root/.m2/repository \
    mvdbos/corda-zk-notary \
    bash -c "cd /src && export PEPPER_BIN_PATH=\"/src/notary/bin\" && gradle --no-daemon --info notary:cleanTest notary:test --tests \"${test_name}\""

#!/usr/bin/env bash

cd $(git rev-parse --show-toplevel)/pepper

contract_name=$1
debug_flag=$2 # DEBUG=1, default is DEBUG=0

if [[ -z "${contract_name}" ]]; then echo "Contract name is a required argument"; exit 1; fi

docker run -v "$(pwd)":/opt/pequin/pepper -it mvdbos/corda-zk-notary bash -c "export PEPPER_BIN_PATH=\"/opt/pequin/pepper/bin\" && cd /opt/pequin/pepper && ./compile_contract.sh ${contract_name} ${debug_flag}"

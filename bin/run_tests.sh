#!/usr/bin/env bash

contract_name=$1
debug_flag=$2 # DEBUG=1, default is DEBUG=0

if [[ -z "${contract_name}" ]]; then echo "Contract name is a required argument"; exit 1; fi

sh ./bin/compile_contract.sh ${contract_name} ${debug_flag} && sh ./bin/deploy_contract.sh ${contract_name} && sh ./bin/run_notary_test.sh

#!/usr/bin/env bash

contract_name=$1

if [[ -z "${contract_name}" ]]; then echo "Contract name is a required argument"; exit 1; fi

sh ./bin/deploy_contract.sh ${contract_name} && sh ./bin/run_notary_test.sh

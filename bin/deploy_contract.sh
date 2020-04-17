#!/usr/bin/env bash

cd $(git rev-parse --show-toplevel)

contract_name=$1

if [[ -z "${contract_name}" ]]; then echo "Contract name is a required argument"; exit 1; fi


mkdir -p ./{workflows,notary}/proving_material
mkdir -p ./{workflows,notary}/verification_material
mkdir -p ./{workflows,notary}/prover_verifier_shared
mkdir -p ./{workflows,notary}/src/test/resources

rm -rf ./{workflows,notary}/proving_material/*
rm -rf ./{workflows,notary}/verification_material/*
rm -rf ./{workflows,notary}/prover_verifier_shared/*
rm -f ./{workflows,notary}/src/test/resources/libpv.so

cp -r ./pepper/prover_verifier_shared ./workflows/
cp -r ./pepper/prover_verifier_shared ./notary/

cp ./pepper/bin/${contract_name}.params ./workflows/prover_verifier_shared/
cp ./pepper/bin/${contract_name}.params ./notary/prover_verifier_shared/

cp ./pepper/bin/${contract_name}.pws ./workflows/proving_material/
cp ./pepper/bin/${contract_name}.pws ./notary/proving_material/

cp ./pepper/proving_material/${contract_name}.pkey ./workflows/proving_material/
cp ./pepper/proving_material/${contract_name}.pkey ./notary/proving_material/

cp ./pepper/verification_material/${contract_name}.vkey ./workflows/verification_material/
cp ./pepper/verification_material/${contract_name}.vkey ./notary/verification_material/

# This should also be dynamically named for the contract
cp ./pepper/compiled_libs/libpv.so workflows/src/test/resources/
cp ./pepper/compiled_libs/libpv.so notary/src/test/resources/

## Copy Jsnark circuit files
#cp ./pepper/*.arith ./workflows/bin/
#cp ./pepper/*.arith ./notary/bin/
#cp ./pepper/*.in ./workflows/bin/
#cp ./pepper/*.in ./notary/bin/

# Copy  executables
cp ./pepper/bin/* ./notary/bin/
cp ./pepper/bin/* ./workflows/bin/

./gradlew deployNodes # this one may be quite slow depending on hardware and weather

cd build/nodes/Zk1

nohup java -Dexperimental.corda.customSerializationScheme=com.ing.zkflow.common.serialization.BFLSerializationScheme  -jar ../../../../corda-hacked.jar --allow-hibernate-to-manage-app-schema

echo Sleeping for 1m...
sleep 1m

cd ../../../../sample-zkdapp

./gradlew test --tests com.example.rpc.E2ERpcTest
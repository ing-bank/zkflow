cd .. || exit

./gradlew deployNodes

cd build/nodes/Zk1 || exit

nohup java -Dexperimental.corda.customSerializationScheme=com.ing.zkflow.common.serialization.BFLSerializationScheme  \
    -jar corda.jar --allow-hibernate-to-manage-app-schema
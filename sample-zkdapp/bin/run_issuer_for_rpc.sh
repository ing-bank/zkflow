cd build/nodes/Issuer || exit

nohup java -Dexperimental.corda.customSerializationScheme=com.ing.zkflow.common.serialization.BFLSerializationScheme  \
    -jar corda.jar --allow-hibernate-to-manage-app-schema
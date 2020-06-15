# Limitations compared to normal Corda

* Limited number of inputs/outputs/references (4 each?)
* No support for attachments withing the circuit, too heavy to have as part of 
* Smart contracts and linked cicruit are not in attachments but previously distributed. Tx contains a reference to that contract version
* All states in a transaction must belong to this contract
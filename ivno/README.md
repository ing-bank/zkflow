# Ivno Collateral Backed Tokens

Ivno's collateral backed token CorDapp provides 24/7 instant settlement, real-time payment and immediate collateral mobility.

In order to obtain Ivno collateral backed tokens, a depositor must deposit a collateral asset, such as cash, with a custodian. The custodian places these assets into a fund and authorises a node known as the Token Issuing Entity (TIE) to issue tokens to the depositor to the value of the collateral asset. These tokens can then be freely transferred between financial institutions until such time that a token owner wishes to redeem the tokens for collateral assets. At this time, the token owner creates a redemption request with the TIE, who upon accepting the request, will burn the token, in exchange for collateral assets being withdrawn from the fund, held by the custodian.

## Deposits

The following tables represent truth tables for which participant is expected to sign the transaction, and which participant is expected to be the counter-party for deposit transactions.

| Status                | Depositor     | Custodian     |
| --------------------- | ------------- | ------------- |
| **DEPOSIT_REQUESTED** | Signer        | Counter-party |
| **DEPOSIT_ACCEPTED**  | Counter-party | Signer        |
| **DEPOSIT_REJECTED**  | Counter-party | Signer        |
| **DEPOSIT_CANCELLED** | Signer        | Counter-party |
| **PAYMENT_ISSUED**    | Signer        | Counter-party |
| **PAYMENT_ACCEPTED**  | Counter-party | Signer        |
| **PAYMENT_REJECTED**  | Counter-party | Signer        |

## Transfers

The following tables represent truth tables for which participant is expected to sign the transaction, and which participant is expected to be the counter-party for token transfer transactions.

### Request to Send

Request to **send** tokens from _Sender_ to _Receiver_ (Initiated by _Sender_)

| Status        | Sender        | Receiver      |
| ------------- | ------------- | ------------- |
| **REQUESTED** | Signer        | Counter-party |
| **ACCEPTED**  | Counter-party | Signer        |
| **REJECTED**  | Counter-party | Signer        |
| **CANCELLED** | Signer        | Counter-party |
| **COMPLETED** | Signer        | Counter-party |

### Request to Receive

Request to **receive** tokens from _Sender_ to _Receiver_ (Initiated by _Receiver_)

| Status           | Sender        | Receiver      |
| ---------------- | ------------- | ------------- |
| **REQUESTED**    | Counter-party | Signer        |
| ~~**ACCEPTED**~~ | Signer        | Counter-party |
| **REJECTED**     | Signer        | Counter-party |
| **CANCELLED**    | Counter-party | Signer        |
| **COMPLETED**    | Signer        | Counter-party |


# Setup dev env

## One-off

### Setup AWS
- Must have aws cli installed
- Create profile: `aws configure --profile ivno-dev`
- Enter the access key and secret given to you
- Use `eu-west-1` as region
- Use default for output

### Setup DASL
 - In order to access the jars in gradle, add the following to your `~/.gradle/gradle.properties` file, replacing `LAB577_DEPLOY_TOKEN`
> Note this file is in your home directory - not the root of the repo 
```
577.deploy_token=LAB577_DEPLOY_TOKEN
 ```

## Running the network with Deploy Nodes
### Linux (Ubuntu)
#### One-off steps
- Open a terminal
- Go to Edit > Preferences
- Add a new profile and call it "HoldOpen"
- Go to the `Command` tab and for `When command exists:` select the `Hold the terminal open` option

This will allow you to see any error that cause Corda to crash, rather than the termain just closing.

### Running the network
- `./build-spin-up-local.sh` will build, purge maven local and re-publish to maven local and run the network
- `./spin-up-deploy-nodes.sh` will just deployNodes and run them

# Onboarding a new node into the network
- When a new participant joins the network, no Ivno token type will be available in its vault. This type of state is needed for token transfers and redemption. As part of the onboarding process, the transaction where a specific Ivno token type was created will be published to the desired set of parties using PublishTokenTypeFlow.

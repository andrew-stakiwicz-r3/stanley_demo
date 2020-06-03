<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Digital Exchange Cordapp

The cordapp project is in Cordapp-template-kotlin folder. 

# Running node

See https://docs.corda.net/getting-set-up.html.

To compile the cordapp run "./gradlew clean deployNodes"

To run the nodes, run "./build/nodes/runnodes"
Then the 5 nodes will be run namely exchange, issue1, jp Morgan, citi bank and notary

# Digital Exchange User interface

The project is in DigitalExchange2 folder. It is implemented by JavaFX

If you modified and recompiled the cordapp, you need to copy and paste the contracts-xx.jar and workflow-xx.jar to /DigitalExchange2/cordappLib


# Running User interface

Go to DigitalExchange2/out/artifacts/DigitalExchange2

Run the following 1 by 1:

java -jar ./DigitalExchange2.jar localhost:10006 E 
java -jar ./DigitalExchange2.jar localhost:10009 I
java -jar ./DigitalExchange2.jar localhost:10015 P 
java -jar ./DigitalExchange2.jar localhost:10012 P

The first parameter is the node's RPC IP and port. The second parameter is to set the node's role e.g. "E" means exchange which can issue Fiat token and "I" mean issuer which can issue stock token.
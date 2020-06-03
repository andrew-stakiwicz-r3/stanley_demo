package com.demo.flow

import co.paralleluniverse.fibers.Suspendable

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
//import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.heldTokensByToken
import com.r3.corda.lib.tokens.workflows.utilities.heldTokensByTokenIssuer
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import demo.DividendReceivableContract
import demo.DividendReceivableState
import demo.Stock

import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.utilities.unwrap
import kotlin.math.log

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
@StartableByService
@StartableByRPC
@InitiatingFlow

class GetDividendPayment() : FlowLogic<String>() {

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new IOU.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable

        //this flow is to get payment for ALL unpaid Dividend Receivable
        override fun call(): String {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.

            val listOfDRS :List<StateAndRef<DividendReceivableState>> = Helper.getAllDividendReceiviable(serviceHub)
            logger.info("*** # of DRS= ${listOfDRS.size}")
            for (drs_saf in listOfDRS){
                logger.info("**** DRS = ${drs_saf.state.data.toString()}")

                val dividendReceivable = drs_saf.state.data

                val issuerSession = initiateFlow(dividendReceivable.issuer)
                issuerSession.send(dividendReceivable)

                //input
                val inputsFiat = subFlow(ReceiveStateAndRefFlow<FungibleToken>(issuerSession))
                logger.info("************************************ after receiving from issuer 1")


                //output
                val outputsFiat = issuerSession.receive<List<FungibleToken>>().unwrap { it }
                logger.info("************************************ after receiving from issuer 2")
                val outputDividendReceivable = dividendReceivable.copy(isPay = true)


                //txn
                val txCommand = Command(DividendReceivableContract.Commands.Pay(), dividendReceivable.participants.map { it.owningKey })
                val txBuilder = TransactionBuilder(notary)
                        .addInputState(drs_saf)
                        .addOutputState(outputDividendReceivable, DividendReceivableContract.ID)
                        .addCommand(txCommand)

                addMoveTokens(txBuilder, inputsFiat, outputsFiat)



                // Stage 2.
                progressTracker.currentStep = VERIFYING_TRANSACTION
                // Verify that the transaction is valid.
                txBuilder.verify(serviceHub)

                // Stage 3.
                progressTracker.currentStep = SIGNING_TRANSACTION
                // Sign the transaction.
                val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

                // Stage 4.
                progressTracker.currentStep = GATHERING_SIGS
                // Send the state to the counterparty, and receive it back with their signature.
                //val otherPartySession = initiateFlow(dividendReceivable.issuer)
                logger.info("************************************ subFlow before fullySigned")
                val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(issuerSession), GATHERING_SIGS.childProgressTracker()))

                // Stage 5.
                progressTracker.currentStep = FINALISING_TRANSACTION
                // Notarise and record the transaction in both parties' vaults.
                logger.info("************************************ subFlow before Finality")
                subFlow(FinalityFlow(fullySignedTx, setOf(issuerSession), FINALISING_TRANSACTION.childProgressTracker()))

            }

            return "done"

        }
    }

    @InitiatedBy(GetDividendPayment::class)
    class GetDividendPaymentHandler(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {

            val dividendReceivable = otherPartySession.receive<DividendReceivableState>().unwrap { it }
            logger.info("************************************ 2 dividendR=${dividendReceivable.toString()}")
            val (inputsFiat, outputsFiat) = DatabaseTokenSelection(serviceHub).generateMove(
                    //logger.info("**** GetDividendPayment 3")

                    listOf(Pair(dividendReceivable.holder, dividendReceivable.dividendAmount)),
                    ourIdentity,
                    lockId = runId.uuid
            )


            logger.info("************************************ 3 input=${inputsFiat.toString()}")
            subFlow(SendStateAndRefFlow(otherPartySession, inputsFiat))
            logger.info("************************************ 3 output=${outputsFiat.toString()}")
            otherPartySession.send(outputsFiat)

            logger.info("************************************ 4")




            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    //val input = stx.tx.inputs.single()

                    //val output = stx.tx.outputs.single().data
                    val output = stx.tx.outputsOfType<DividendReceivableState>().single()
                    "This must be an dividend transaction." using (output is DividendReceivableState)
                    val drs = output as DividendReceivableState
                    logger.info("***DRS=" + drs.toString())
                    //"I won't accept IOUs with a value over 100." using (iou.value <= 100)
                }
            }
            logger.info("************************************ 5")
            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
            //logger.info("************************************ 6")


        }
    }


package com.demo.flow

import co.paralleluniverse.fibers.Suspendable

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
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


class ClaimDividendReceivable (val symbol: String, val issuer: Party) : FlowLogic<SignedTransaction>() {



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
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.

            //retrieve stock and calcualte required dividend value
            val stockPointer = Helper.getStockPointer(serviceHub, symbol)
            val stockTokenType = stockPointer.pointer.resolve(serviceHub).state.data
            val stockAmount: Amount<TokenType> = serviceHub.vaultService.tokenBalance(stockPointer)
            val dividendAmount = stockTokenType.dividend * serviceHub.vaultService.tokenBalance(stockPointer).quantity.toBigDecimal()
            logger.info("***** dividendAmount= $dividendAmount")


            //same stock token for input and output
            val (stockTokenInputs, stockTokenOutputs) = DatabaseTokenSelection(serviceHub).generateMove(
                    //lockId = runId.uuid,
                   // partyAndAmounts = listOf(PartyAndAmount(ourIdentity, stockAmount)),
                   // changeHolder = ourIdentity

                    listOf(Pair(ourIdentity, stockAmount)),
                    ourIdentity,
                    lockId = runId.uuid
            )

            //output
            val dividendReceivable = DividendReceivableState(issuer=issuer, holder=ourIdentity,
                    payDate = stockTokenType.payDate, dividendAmount = dividendAmount of FiatCurrency.getInstance(stockTokenType.currency))


            //prepare txn, put all input & output into txBuilders
            val txCommand = Command(DividendReceivableContract.Commands.Create(), dividendReceivable.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(dividendReceivable, DividendReceivableContract.ID)
                    .addCommand(txCommand)

            addMoveTokens(txBuilder, stockTokenInputs, stockTokenOutputs)





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
            val otherPartySession = initiateFlow(issuer)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(ClaimDividendReceivable::class)
    class ClaimDividendReceivableHandler(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                    val output = stx.tx.outputsOfType<DividendReceivableState>().single()
                    "This must be an dividend transaction." using (output is DividendReceivableState)
                    val drs = output as DividendReceivableState
                    logger.info("***DRS=" + drs.toString())

                }
            }
            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }


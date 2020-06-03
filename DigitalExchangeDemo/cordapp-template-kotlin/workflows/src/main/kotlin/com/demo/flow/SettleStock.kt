package com.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.move.*
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler

//import com.r3.corda.lib.tokens.workflows.utilities.
//import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection

import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import demo.Stock
import net.corda.core.contracts.Amount
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

import net.corda.core.flows.*
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.math.BigDecimal




@CordaSerializable
data class ExecutionNotification(val buyer: Party, val seller: Party,
                                 val settleStockTokenAmount: Amount<TokenType>, val executedPrice: BigDecimal, val settleFiatTokenAmount: Amount<TokenType>)


@StartableByService
@StartableByRPC
@InitiatingFlow
class SettleStock(val buyer: Party, val seller: Party,
                  val symbol: String, val executedQty: Int, val executedPrice: BigDecimal) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        //get stock
        val stockPointer: TokenPointer<Stock> = Helper.getStockPointer(serviceHub, symbol)
        val stock = stockPointer.pointer.resolve(serviceHub).state.data

        val buyerSession = initiateFlow(buyer)
        val sellerSession = initiateFlow(seller)
        // Ask for input stateAndRefs - send notification with the amount to exchange.

        //prepare info for ExecutionNotification
        val settleStockTokenAmount: Amount<TokenType> = executedQty of stockPointer
        val settleFiatTokenAmount: Amount<TokenType> =  (executedQty.toBigDecimal() * executedPrice) of FiatCurrency.getInstance(stock.currency)

        //get input/output from buyer
        buyerSession.send(ExecutionNotification(buyer, seller, settleStockTokenAmount, executedPrice, settleFiatTokenAmount))

        // TODO add some checks for inputs and outputs
        val inputsFiat = subFlow(ReceiveStateAndRefFlow<FungibleToken>(buyerSession))
        // Receive outputs (this is just quick and dirty, we could calculate them on our side of the flow).
        val outputsFiat = buyerSession.receive<List<FungibleToken>>().unwrap { it }



        //get input/output from buyer
        sellerSession.send(ExecutionNotification(buyer, seller, settleStockTokenAmount, executedPrice, settleFiatTokenAmount))
        val inputsStock = subFlow(ReceiveStateAndRefFlow<FungibleToken>(sellerSession))
        // Receive outputs (this is just quick and dirty, we could calculate them on our side of the flow).
        val outputsStock = sellerSession.receive<List<FungibleToken>>().unwrap { it }



        //txn
        val txBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        addMoveTokens(txBuilder, inputsFiat, outputsFiat)
        addMoveTokens(txBuilder, inputsStock, outputsStock)


        val initialStx = serviceHub.signInitialTransaction(txBuilder, ourIdentity.owningKey)
        val stx = subFlow(CollectSignaturesFlow(initialStx, listOf(buyerSession, sellerSession), listOf(ourIdentity.owningKey)))

         //Update distribution list.
       // ????subFlow(UpdateDistributionListFlow(stx))
       // return subFlow(ObserverAwareFinalityFlow(stx, listOf(buyerSession)))
        return subFlow(ObserverAwareFinalityFlow(stx, listOf(buyerSession, sellerSession)))
    }
}

/**
 * TODO docs
 */
@InitiatedBy(SettleStock::class)
class SettleStockTokensHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
   // println("testing 3")
    override fun call() {
        val executionNotification = otherSession.receive<ExecutionNotification>().unwrap { it }

        if (executionNotification.buyer.equals(ourIdentity)) {
            /*val (inputs, outputs) = TokenSelection(serviceHub).generateMove(
                    lockId = runId.uuid,
                    partyAndAmounts = listOf(PartyAndAmount(executionNotification.seller, executionNotification.settleFiatTokenAmount)),
                    changeHolder = ourIdentity
            )*/

            val (inputs, outputs) = DatabaseTokenSelection(serviceHub).generateMove(

                    listOf(Pair(executionNotification.seller, executionNotification.settleFiatTokenAmount)),
                    ourIdentity,
                    lockId = runId.uuid
            )
            subFlow(SendStateAndRefFlow(otherSession, inputs))
            otherSession.send(outputs)
            //subFlow(IdentitySyncFlow.Receive(otherSession))
            subFlow(object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) {}
            }
            )
            subFlow(ObserverAwareFinalityFlowHandler(otherSession))
        }

        if (executionNotification.seller.equals(ourIdentity)) {


            val (inputs, outputs) =  DatabaseTokenSelection(serviceHub).generateMove(

                    listOf(Pair(executionNotification.buyer, executionNotification.settleStockTokenAmount)),
                    ourIdentity,
                    lockId = runId.uuid

            )
            subFlow(SendStateAndRefFlow(otherSession, inputs))
            otherSession.send(outputs)
            //subFlow(IdentitySyncFlow.Receive(otherSession))
            subFlow(object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) {}
            }
            )
            subFlow(ObserverAwareFinalityFlowHandler(otherSession))
        }

    }

}
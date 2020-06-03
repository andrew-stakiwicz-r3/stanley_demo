package com.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import demo.Stock
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

import net.corda.core.flows.*


@StartableByService
@StartableByRPC
@InitiatingFlow

class MoveStock(val symbol: String, val amount: Long, val recipient: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val stockPointer: TokenPointer<Stock> = Helper.getStockPointer(serviceHub, symbol)


        val session = initiateFlow(recipient)


        return subFlow(MoveFungibleTokensFlow(listOf(PartyAndAmount(recipient, amount of stockPointer)), listOf(session)))
    }
}

/**
 * TODO docs
 */
@InitiatedBy(MoveStock::class)
class MoveStockTokensHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable

    override fun call() = subFlow(MoveTokensFlowHandler(otherSession))
}
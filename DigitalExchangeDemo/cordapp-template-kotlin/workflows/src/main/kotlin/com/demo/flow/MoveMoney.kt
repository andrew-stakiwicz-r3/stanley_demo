package com.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

import net.corda.core.flows.*


@StartableByService
@StartableByRPC
@InitiatingFlow

class MoveMoney(val currency: String, val amount: Long, val recipient: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val token = FiatCurrency.getInstance(currency)
        val session = initiateFlow(recipient)
        return subFlow(MoveFungibleTokensFlow(listOf(PartyAndAmount(recipient, amount of token)), listOf(session)))
    }
}

/**
 * TODO docs
 */
@InitiatedBy(MoveMoney::class)
class MoveFungibleTokensHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable

    override fun call() = subFlow(MoveTokensFlowHandler(otherSession))
}
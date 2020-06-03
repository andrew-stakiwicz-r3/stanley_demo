package com.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow
//import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import demo.Stock
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder
import java.math.BigDecimal
import java.util.*


@StartableByService
@StartableByRPC
@InitiatingFlow
class AnnounceDividend(val symbol: String, val amount: BigDecimal, val exDate: Date, val payDate: Date) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        //prepare input
        val stockPointer: TokenPointer<Stock> = Helper.getStockPointer(serviceHub, symbol)
        val stockRefAndState = stockPointer.pointer.resolve(serviceHub)

        //prepare output
        val output = stockRefAndState.state.data.copy(dividend = amount,
                            exDate = exDate, payDate = payDate)

        //observer sessions
        val exchangeSession = initiateFlow(serviceHub.identityService.partiesFromName("Exchange", false).single())
        val citiBankSession = initiateFlow(serviceHub.identityService.partiesFromName("Citi Bank", false).single())
        val jpMorganSession = initiateFlow(serviceHub.identityService.partiesFromName("JP Morgan", false).single())


        // val listOfSessions = serviceHub.identityService.getAllIdentities().map { initiateFlow(it.party) }

        // here use observer approach to send the stock update to exchange and all participants.
        // One of the better design is the participant to request the update before market open by using sendTransactionFlow
        return subFlow(UpdateEvolvableTokenFlow(stockRefAndState, output, emptyList() , listOf(exchangeSession, citiBankSession, jpMorganSession)))

    }


}

@InitiatedBy(AnnounceDividend::class)
class UpdateAnnounceDividend(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() = subFlow(UpdateEvolvableTokenFlowHandler(otherSession))
}

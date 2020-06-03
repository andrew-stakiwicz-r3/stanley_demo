package com.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import demo.Stock
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import java.math.BigDecimal
import java.util.*

@StartableByRPC
class IssueStock(val symbol: String, val name: String, val currency: String, val issueVol: Int) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val exchange = serviceHub.identityService.partiesFromName("Exchange", false).single()
        val citi = serviceHub.identityService.partiesFromName("Citi Bank", false).single()
        val jp = serviceHub.identityService.partiesFromName("JP Morgan", false).single()

        val stock = Stock(symbol= symbol, name= name, currency = currency,
                dividend = BigDecimal(0),
                exDate = Date(),
                payDate = Date(),
                maintainers = listOf(ourIdentity))
       subFlow(CreateEvolvableTokens(stock.withNotary(getPreferredNotary(serviceHub)),listOf(exchange, citi, jp)))



        val stockPointer: TokenPointer<Stock> = stock.toPointer()

        val issuedStock = stockPointer issuedBy ourIdentity



        return subFlow(IssueTokens(listOf(issueVol of issuedStock heldBy ourIdentity), listOf(exchange, citi, jp)))



    }
}

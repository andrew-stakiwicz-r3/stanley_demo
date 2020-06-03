package com.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.addPartyToDistributionList
import com.r3.corda.lib.tokens.workflows.utilities.participants
import demo.Stock
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import java.util.*


@StartableByService
@StartableByRPC
@InitiatingFlow
class getFiatTokenBalance(val currency: String) : FlowLogic<Amount<TokenType>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): Amount<TokenType> {


        val tokenType = FiatCurrency.getInstance(currency)
        println("stanley - testing")

      //  return "Token Balance(" + currency + ")=" + serviceHub.vaultService.tokenBalance(tokenType).toString()
        return serviceHub.vaultService.tokenBalance(tokenType)



    }
}

@StartableByService
@StartableByRPC
@InitiatingFlow
class getStockTokenBalance(val symbol: String) : FlowLogic<Amount<TokenType>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): Amount<TokenType> {


/*
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)

        val results = builder {
            //val currencyIndex = Stock::symbol.equal(symbol)
            //val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(currencyIndex)
            //val criteria = generalCriteria.and(customCriteria1)
            serviceHub.vaultService.queryBy (Stock::class.java, generalCriteria)
        }
        for (item in results.states){
            if (symbol.equals(item.state.data.symbol)){
                val stockPointer: TokenPointer<Stock> = item.state.data.toPointer()
                return serviceHub.vaultService.tokenBalance(stockPointer).toString()
            }
        }
*/
        val stockPointer: TokenPointer<Stock>? = Helper.getStockPointer(serviceHub, symbol)
        if (stockPointer != null) {
            return serviceHub.vaultService.tokenBalance(stockPointer)
        } else {
            return null!!

        }
    }
}

@StartableByService
@StartableByRPC
@InitiatingFlow
class getStockDetail(val symbol: String) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call(): String {
            val stockPointer: TokenPointer<Stock> = Helper.getStockPointer(serviceHub, symbol)

            val stockRefAndState = stockPointer.pointer.resolve(serviceHub)
            return stockRefAndState.state.data.toString()
        }

}



@StartableByService
@StartableByRPC
@InitiatingFlow
class getTransactions() : FlowLogic<String>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): String {

//val input = tx.inputsOfType<Obligation>().single()
//        val output = tx.outputsOfType<Obligation>().single()
        val msg: String = "******************\n"

        for (item in serviceHub.validatedTransactions.track().snapshot){
            logger.info("*****************")
            logger.info("Txn hash: " + item.tx.toLedgerTransaction(serviceHub).hashCode())
            if (item.tx.toLedgerTransaction(serviceHub).inputStates.size >0) {
                logger.info("***input class - " +item.tx.toLedgerTransaction(serviceHub).inputStates[0].javaClass.toString())
                logger.info("***input state - " +item.tx.toLedgerTransaction(serviceHub).inputStates[0].toString())
                msg.plus("input: ")
                msg.plus(item.tx.toLedgerTransaction(serviceHub).inputStates[0].toString())
                msg.plus("\n")
            }
            if (item.tx.outputStates.size >0) {
                logger.info("***output class - " +item.tx.outputStates[0].javaClass.toString())
                logger.info("***output state -" + item.tx.outputStates[0].toString())
                msg.plus("output: ")
                msg.plus(item.tx.toLedgerTransaction(serviceHub).outputStates[0].toString())
                msg.plus("\n")
            }
            for (p in item.tx.toLedgerTransaction(serviceHub).participants){
                logger.info("Participant:" + serviceHub.identityService.partyFromKey(p.owningKey)!!.name.toString())
                msg.plus("participants: ")
                msg.plus(serviceHub.identityService.partyFromKey(p.owningKey)!!.name.toString())
                msg.plus("\n")
            }
            logger.info("*****************")

        }
       // logger.info(msg)
        return msg
    }
}


@StartableByService
@StartableByRPC
@InitiatingFlow
class UpdateDistributionList(val symbol: String) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): String {

        val stockPointer: TokenPointer<Stock> = Helper.getStockPointer(serviceHub, symbol)
        val stock = stockPointer.pointer.resolve(serviceHub).state.data
        if (stock.symbol.equals("")){
            return "cannot find the symbol from ledger"
        }
        serviceHub.identityService.getAllIdentities().forEach {
            logger.info("****** Party=" + it.party.name)
            //logger.info("****** Party=" + it.party.name)

            serviceHub.addPartyToDistributionList(party = it.party, linearId = stock.linearId)

        }
        return "stock=" + stock.toString()

    }
}
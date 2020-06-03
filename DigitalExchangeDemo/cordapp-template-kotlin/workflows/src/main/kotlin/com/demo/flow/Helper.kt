package com.demo.flow

import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import demo.DividendReceivableState
import demo.Stock
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.internal.WaitForStateConsumption.Companion.logger
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import java.math.BigDecimal


class Helper {
    companion object {
        // Uses the java money registry.
        fun getStockPointer(serviceHub: ServiceHub, symbol: String): TokenPointer<Stock> {
            val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)

            val results = builder {
                //val currencyIndex = Stock::symbol.equal(symbol)
                //val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(currencyIndex)
                //val criteria = generalCriteria.and(customCriteria1)
                serviceHub.vaultService.queryBy (Stock::class.java, generalCriteria)
            }
            for (item in results.states){
                if (symbol.equals(item.state.data.symbol)){
                    val stockPointer: TokenPointer<Stock> = item.state.data.toPointer()
                   // stockPointer.pointer.resolve(serviceHub)
                    return stockPointer
                }
            }
           // val dividend: Amount<TokenType> = 0 of FiatCurrency.getInstance("HKD")
            val stock = Stock(symbol= "", name= "", dividend = BigDecimal(0), currency = "", maintainers = emptyList())

            return stock.toPointer()
        }

        fun getAllDividendReceiviable(serviceHub: ServiceHub): List<StateAndRef<DividendReceivableState>>{
            val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)

            val results = builder {
                //val currencyIndex = DividendReceivableState::isPay.equal(false)
                //val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(currencyIndex)
               // val criteria = generalCriteria.and(customCriteria1)
                logger.info("before queryBy")
                serviceHub.vaultService.queryBy (DividendReceivableState::class.java, generalCriteria)
            }
            logger.info("before filter")
            return results.states.filter { it.state.data.isPay==false}
            /*
            for (item in results.states){
                if (symbol.equals(item.state.data.symbol)){
                    val stockPointer: TokenPointer<Stock> = item.state.data.toPointer()
                    return stockPointer
                }
            }*/
        }
    }
}
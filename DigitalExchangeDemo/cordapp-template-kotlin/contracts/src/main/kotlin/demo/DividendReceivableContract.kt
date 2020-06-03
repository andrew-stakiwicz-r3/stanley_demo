package demo

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.internal.WaitForStateConsumption.Companion.logger
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


class DividendReceivableContract : Contract {
    companion object {
        @JvmStatic
        val ID = "demo.DividendReceivableContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()

        when (command.value) {
            is Commands.Create -> verifyIssue(tx, setOfSigners)
            is Commands.Pay -> verifyPay(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }

    }
    private fun verifyIssue(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {

        val out = tx.outputsOfType<DividendReceivableState>().single()
        "All of the participants must be signers." using (signers.containsAll(out.participants.map { it.owningKey }))

     //   for (item in tx.inputStates){
       //    logger.info("********* verifyIssue input state= " + item.toString())
         //  logger.info("********* verifyIssue input state class= " + item.javaClass.toString())
          //  400 TokenPointer(class demo.Stock, 84063973-7dc1-4669-85bb-3c045c12fb25) issued by Issuer1 held by Citi Bank {fiber-id=10000029, flow-id=0a7c7e46-893c-45db-8cc8-97dfb18f55f8, invocation_id=4a2af986-599d-4941-8b36-cb92e70e8b07, invocation_timestamp=2019-08-13T03:46:15.879Z, origin=O=Citi Bank, L=HK, C=HK, session_id=4a2af986-599d-4941-8b36-cb92e70e8b07, session_timestamp=2019-08-13T03:46:15.879Z, thread-id=259
        //}
        // Below is good example how to get the stock from underlying token
        // The dividend receivable cannot be created before ex-date. For demo purpose, it is commented
        /*
        val input = tx.inputsOfType<FungibleToken>().single()
        logger.info("TokenType=" +input.tokenType.toString() )
        logger.info("TokenTypeClass=" +input.tokenType.tokenClass.toString() )
        logger.info("Issued TokenType=" +input.issuedTokenType.toString() )


       val stockPointer :TokenPointer<Stock>    = input.tokenType as TokenPointer<Stock>
        val stock = stockPointer.pointer.resolve(tx).state.data
        logger.info("StockPointer=" + stock.toString())

        var curDateStr = SimpleDateFormat("dd/MM/yyyy").format(Date())
        var curDate2 = SimpleDateFormat("dd/MM/yyyy").parse(curDateStr)
        logger.info("date2=" + curDate2.toString())

        "You can't get Dividend Receivable before ex-date" using (stock.exDate.before(curDate2))
*/




    }
    private fun verifyPay(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        //the payment should be made on or after paydate. For simplicity, i only check who should be the signer

        val out = tx.outputsOfType<DividendReceivableState>().single()
        "All of the participants must be signers." using (signers.containsAll(out.participants.map { it.owningKey }))

    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
        class Pay: Commands
    }
}

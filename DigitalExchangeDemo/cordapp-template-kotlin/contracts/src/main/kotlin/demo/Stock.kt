package demo

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.template.ExampleEvolvableTokenTypeContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.schemas.StatePersistable
import java.math.BigDecimal
import java.util.*


@BelongsToContract(StockContract::class)
data class Stock(
        val symbol: String,
        val name: String,
        val currency: String,
        val dividend: BigDecimal,
        val exDate: Date = Date(),
        val payDate: Date = Date(),
        //val displayTokenSize: BigDecimal,
        override val maintainers: List<Party>,
        override val linearId: UniqueIdentifier= UniqueIdentifier(),
        override val fractionDigits: Int = 0

) : EvolvableTokenType(), StatePersistable {

//EvolvableTokenType(), QueryableState, StatePersistable {


    //override val maintainers: List<Party> get() = listOf(maintainer)
/*
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is StockSchemaV1 -> StockSchemaV1.PersistentStock(

                    this.symbol.toString(),
                    this.name.toString(),
                    this.currency.toString(),

                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(StockSchemaV1)

    */
}
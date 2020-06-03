package demo


import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.schemas.StatePersistable
import java.util.*

@BelongsToContract(DividendReceivableContract::class)
data class DividendReceivableState(val issuer: Party,
                                   val holder: Party,
                                   val payDate: Date,
                                   val dividendAmount: Amount<TokenType>,
                                   val isPay: Boolean = false,
                                   override val linearId: UniqueIdentifier = UniqueIdentifier())
    :LinearState, QueryableState, StatePersistable {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(issuer, holder)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is DRSSchemaV1 -> DRSSchemaV1.PersistentDRS(
                    this.issuer.name.toString(),
                    this.holder.name.toString(),
                    this.payDate,
                    this.dividendAmount.quantity,
                    this.isPay,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(DRSSchemaV1)
}
package demo


import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for IOUState.
 */
object StockSchema

/**
 * An IOUState schema.
 */
object StockSchemaV1 : MappedSchema(
        schemaFamily = StockSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentStock::class.java)) {
    @Entity
    @Table(name = "stock_states")
    class PersistentStock(
            @Column(name = "symbol")
            var symbol: String,

            @Column(name = "name")
            var name: String,


            @Column(name = "currency")
            var currency: String,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "", UUID.randomUUID())
    }
}
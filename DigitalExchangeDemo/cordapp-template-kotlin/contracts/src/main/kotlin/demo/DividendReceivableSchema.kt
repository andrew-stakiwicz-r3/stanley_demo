package demo

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.math.BigDecimal
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for IOUState.
 */
object DRSSchema

/**
 * An IOUState schema.
 */
object DRSSchemaV1 : MappedSchema(
        schemaFamily = DRSSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentDRS::class.java)) {
    @Entity
    @Table(name = "dividend_receivable")
    class PersistentDRS(
            @Column(name = "issuer")
            var issuer: String,

            @Column(name = "holder")
            var holder: String,

            @Column(name = "pay_date")
            var payDate: Date,

            @Column(name = "dividend_amount")
            var dividendAmount: Long,

            @Column(name = "is_pay")
            var isPay: Boolean,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", Date(), 0, false, UUID.randomUUID())
    }
}